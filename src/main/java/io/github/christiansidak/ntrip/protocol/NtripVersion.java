package io.github.christiansidak.ntrip.protocol;

/**
 * NTRIP protocol version.
 */
public enum NtripVersion {

    /** NTRIP v1: HTTP/1.0 with ICY 200 OK response */
    V1("Ntrip/1.0"),

    /** NTRIP v2: HTTP/1.1 with chunked transfer encoding */
    V2("Ntrip/2.0");

    private final String userAgentToken;

    NtripVersion(String userAgentToken) {
        this.userAgentToken = userAgentToken;
    }

    public String getUserAgentToken() {
        return userAgentToken;
    }
}
