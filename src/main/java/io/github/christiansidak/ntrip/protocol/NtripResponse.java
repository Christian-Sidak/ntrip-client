package io.github.christiansidak.ntrip.protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses the NTRIP response status line and headers.
 *
 * NTRIP v1 responds with "ICY 200 OK" (not standard HTTP).
 * NTRIP v2 responds with standard "HTTP/1.1 200 OK".
 */
public final class NtripResponse {

    private final int statusCode;
    private final String statusLine;
    private final Map<String, String> headers;

    private NtripResponse(int statusCode, String statusLine, Map<String, String> headers) {
        this.statusCode = statusCode;
        this.statusLine = statusLine;
        this.headers = Collections.unmodifiableMap(headers);
    }

    /**
     * Read the response status line and headers from the input stream.
     * After this call, the stream is positioned at the start of the body.
     */
    public static NtripResponse parse(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.US_ASCII));

        String statusLine = reader.readLine();
        if (statusLine == null || statusLine.isEmpty()) {
            throw new IOException("Empty response from NTRIP caster");
        }

        int statusCode = parseStatusCode(statusLine);

        Map<String, String> headers = new LinkedHashMap<>();
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            int colon = line.indexOf(':');
            if (colon > 0) {
                String key = line.substring(0, colon).trim().toLowerCase();
                String value = line.substring(colon + 1).trim();
                headers.put(key, value);
            }
        }

        return new NtripResponse(statusCode, statusLine, headers);
    }

    private static int parseStatusCode(String statusLine) throws IOException {
        // Handle both "ICY 200 OK" (v1) and "HTTP/1.1 200 OK" (v2)
        String[] parts = statusLine.split("\\s+", 3);
        if (parts.length < 2) {
            throw new IOException("Malformed NTRIP response: " + statusLine);
        }
        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid status code in response: " + statusLine);
        }
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusLine() {
        return statusLine;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getHeader(String name) {
        return headers.get(name.toLowerCase());
    }

    public boolean isSuccess() {
        return statusCode == 200;
    }

    public boolean isUnauthorized() {
        return statusCode == 401;
    }

    public boolean isSourcetable() {
        String contentType = getHeader("content-type");
        return contentType != null && contentType.contains("gnss/sourcetable");
    }
}
