package io.github.christiansidak.ntrip.protocol;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class NtripRequestTest {

    @Test
    void v1SourcetableRequest() {
        byte[] request = NtripRequest.sourcetable("rtk2go.com", 2101, NtripVersion.V1, null, null);
        String str = new String(request, StandardCharsets.US_ASCII);

        assertTrue(str.startsWith("GET / HTTP/1.0\r\n"));
        assertTrue(str.contains("User-Agent: NTRIP ntrip-client/0.1.0\r\n"));
        assertFalse(str.contains("Authorization"));
        assertTrue(str.endsWith("\r\n\r\n"));
    }

    @Test
    void v2StreamRequestWithAuth() {
        byte[] request = NtripRequest.stream("caster.example.com", 2101, "MY_MOUNT",
                NtripVersion.V2, "user", "pass", null);
        String str = new String(request, StandardCharsets.US_ASCII);

        assertTrue(str.startsWith("GET /MY_MOUNT HTTP/1.1\r\n"));
        assertTrue(str.contains("Host: caster.example.com\r\n"));
        assertTrue(str.contains("Ntrip-Version: Ntrip/2.0\r\n"));
        assertTrue(str.contains("Authorization: Basic dXNlcjpwYXNz\r\n")); // base64("user:pass")
        assertFalse(str.contains("Ntrip-GGA"));
    }

    @Test
    void includesGgaSentence() {
        String gga = "$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,47.0,M,,*47";
        byte[] request = NtripRequest.stream("host", 2101, "MOUNT",
                NtripVersion.V2, "u", "p", gga);
        String str = new String(request, StandardCharsets.US_ASCII);

        assertTrue(str.contains("Ntrip-GGA: " + gga + "\r\n"));
    }

    @Test
    void nonDefaultPortIncludedInHost() {
        byte[] request = NtripRequest.stream("host", 8080, "MOUNT",
                NtripVersion.V2, null, null, null);
        String str = new String(request, StandardCharsets.US_ASCII);

        assertTrue(str.contains("Host: host:8080\r\n"));
    }

    @Test
    void mountpointSlashHandling() {
        byte[] withSlash = NtripRequest.stream("h", 2101, "/MOUNT",
                NtripVersion.V1, null, null, null);
        byte[] withoutSlash = NtripRequest.stream("h", 2101, "MOUNT",
                NtripVersion.V1, null, null, null);

        String str1 = new String(withSlash, StandardCharsets.US_ASCII);
        String str2 = new String(withoutSlash, StandardCharsets.US_ASCII);

        assertTrue(str1.contains("GET /MOUNT "));
        assertTrue(str2.contains("GET /MOUNT "));
    }
}
