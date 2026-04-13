package io.github.christiansidak.ntrip.protocol;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Builds raw NTRIP request bytes for v1 and v2 connections.
 *
 * NTRIP v1 uses a bare HTTP/1.0-style request with ICY protocol.
 * NTRIP v2 uses standard HTTP/1.1 with Ntrip-Version header and chunked transfer.
 */
public final class NtripRequest {

    private NtripRequest() {}

    /**
     * Build a sourcetable request (GET / with no mountpoint).
     */
    public static byte[] sourcetable(String host, int port, NtripVersion version,
                                     String username, String password) {
        return build(host, port, "/", version, username, password, null);
    }

    /**
     * Build a stream request for a specific mountpoint.
     */
    public static byte[] stream(String host, int port, String mountpoint, NtripVersion version,
                                String username, String password, String nmeaGga) {
        String path = mountpoint.startsWith("/") ? mountpoint : "/" + mountpoint;
        return build(host, port, path, version, username, password, nmeaGga);
    }

    private static byte[] build(String host, int port, String path, NtripVersion version,
                                String username, String password, String nmeaGga) {
        StringBuilder sb = new StringBuilder();

        if (version == NtripVersion.V1) {
            sb.append("GET ").append(path).append(" HTTP/1.0\r\n");
            sb.append("User-Agent: NTRIP ntrip-client/0.1.0\r\n");
            sb.append("Accept: */*\r\n");
        } else {
            sb.append("GET ").append(path).append(" HTTP/1.1\r\n");
            sb.append("Host: ").append(host);
            if (port != 2101) {
                sb.append(":").append(port);
            }
            sb.append("\r\n");
            sb.append("Ntrip-Version: ").append(version.getUserAgentToken()).append("\r\n");
            sb.append("User-Agent: NTRIP ntrip-client/0.1.0\r\n");
            sb.append("Accept: */*\r\n");
            sb.append("Connection: close\r\n");
        }

        if (username != null && !username.isEmpty()) {
            String credentials = username + ":" + (password != null ? password : "");
            String encoded = Base64.getEncoder().encodeToString(
                    credentials.getBytes(StandardCharsets.UTF_8));
            sb.append("Authorization: Basic ").append(encoded).append("\r\n");
        }

        if (nmeaGga != null && !nmeaGga.isEmpty()) {
            sb.append("Ntrip-GGA: ").append(nmeaGga).append("\r\n");
        }

        sb.append("\r\n");
        return sb.toString().getBytes(StandardCharsets.US_ASCII);
    }
}
