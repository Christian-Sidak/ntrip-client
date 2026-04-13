package io.github.christiansidak.ntrip.protocol;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class NtripResponseTest {

    @Test
    void parsesV1Response() throws IOException {
        String raw = "ICY 200 OK\r\n"
                + "Content-Type: gnss/data\r\n"
                + "Server: NTRIP Caster\r\n"
                + "\r\n";
        NtripResponse resp = parse(raw);

        assertEquals(200, resp.getStatusCode());
        assertTrue(resp.isSuccess());
        assertEquals("gnss/data", resp.getHeader("Content-Type"));
    }

    @Test
    void parsesV2Response() throws IOException {
        String raw = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: gnss/sourcetable\r\n"
                + "Ntrip-Version: Ntrip/2.0\r\n"
                + "\r\n";
        NtripResponse resp = parse(raw);

        assertTrue(resp.isSuccess());
        assertTrue(resp.isSourcetable());
    }

    @Test
    void detectsUnauthorized() throws IOException {
        String raw = "HTTP/1.1 401 Unauthorized\r\n\r\n";
        NtripResponse resp = parse(raw);

        assertTrue(resp.isUnauthorized());
        assertFalse(resp.isSuccess());
    }

    @Test
    void headerLookupCaseInsensitive() throws IOException {
        String raw = "ICY 200 OK\r\nContent-Type: gnss/data\r\n\r\n";
        NtripResponse resp = parse(raw);

        assertEquals("gnss/data", resp.getHeader("content-type"));
        assertEquals("gnss/data", resp.getHeader("Content-Type"));
        assertEquals("gnss/data", resp.getHeader("CONTENT-TYPE"));
    }

    @Test
    void throwsOnEmptyResponse() {
        assertThrows(IOException.class, () -> parse(""));
    }

    private NtripResponse parse(String raw) throws IOException {
        return NtripResponse.parse(
                new ByteArrayInputStream(raw.getBytes(StandardCharsets.US_ASCII)));
    }
}
