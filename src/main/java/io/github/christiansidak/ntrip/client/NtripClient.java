package io.github.christiansidak.ntrip.client;

import io.github.christiansidak.ntrip.protocol.NtripRequest;
import io.github.christiansidak.ntrip.protocol.NtripResponse;
import io.github.christiansidak.ntrip.protocol.NtripVersion;
import io.github.christiansidak.ntrip.rtcm.RtcmDecoder;
import io.github.christiansidak.ntrip.rtcm.RtcmFrame;
import io.github.christiansidak.ntrip.sourcetable.Sourcetable;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * NTRIP client for connecting to NTRIP casters and receiving GNSS correction data.
 *
 * Supports NTRIP v1 (ICY) and v2 (HTTP/1.1) protocols.
 *
 * <pre>{@code
 * NtripClient client = new NtripClient.Builder("rtk2go.com", 2101)
 *     .credentials("user", "password")
 *     .version(NtripVersion.V1)
 *     .build();
 *
 * // List available mountpoints
 * Sourcetable table = client.getSourcetable();
 * table.getStreams().forEach(System.out::println);
 *
 * // Stream RTCM corrections
 * client.stream("MOUNTPOINT", frame -> {
 *     System.out.println("RTCM type " + frame.getMessageType());
 * });
 * }</pre>
 */
public class NtripClient implements Closeable {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final NtripVersion version;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    private volatile Socket socket;
    private volatile boolean closed;

    private NtripClient(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.username = builder.username;
        this.password = builder.password;
        this.version = builder.version;
        this.connectTimeoutMs = builder.connectTimeoutMs;
        this.readTimeoutMs = builder.readTimeoutMs;
    }

    /**
     * Fetch and parse the caster's sourcetable.
     */
    public Sourcetable getSourcetable() throws IOException {
        Socket sock = connect();
        try {
            OutputStream out = sock.getOutputStream();
            out.write(NtripRequest.sourcetable(host, port, version, username, password));
            out.flush();

            InputStream in = new BufferedInputStream(sock.getInputStream());
            NtripResponse response = NtripResponse.parse(in);

            if (!response.isSuccess()) {
                throw new IOException("Sourcetable request failed: " + response.getStatusLine());
            }

            return Sourcetable.parse(in);
        } finally {
            sock.close();
        }
    }

    /**
     * Connect to a mountpoint and stream raw correction data.
     * Calls the consumer for each chunk of raw bytes received.
     * Blocks until the connection is closed or an error occurs.
     *
     * @param mountpoint the mountpoint name
     * @param consumer callback receiving raw byte chunks
     */
    public void streamRaw(String mountpoint, Consumer<byte[]> consumer) throws IOException {
        streamRaw(mountpoint, null, consumer);
    }

    /**
     * Connect to a mountpoint with an NMEA GGA position and stream raw data.
     * The GGA sentence is needed for Virtual Reference Station (VRS) mountpoints.
     *
     * @param mountpoint the mountpoint name
     * @param nmeaGga NMEA GGA sentence for VRS, or null
     * @param consumer callback receiving raw byte chunks
     */
    public void streamRaw(String mountpoint, String nmeaGga, Consumer<byte[]> consumer)
            throws IOException {
        socket = connect();
        try {
            OutputStream out = socket.getOutputStream();
            out.write(NtripRequest.stream(host, port, mountpoint, version,
                    username, password, nmeaGga));
            out.flush();

            InputStream in = new BufferedInputStream(socket.getInputStream());
            NtripResponse response = NtripResponse.parse(in);

            if (response.isUnauthorized()) {
                throw new IOException("Authentication failed (401). Check credentials.");
            }
            if (response.isSourcetable()) {
                throw new IOException("Mountpoint '" + mountpoint
                        + "' not found. Server returned sourcetable instead.");
            }
            if (!response.isSuccess()) {
                throw new IOException("Stream request failed: " + response.getStatusLine());
            }

            byte[] buf = new byte[4096];
            int n;
            while (!closed && (n = in.read(buf)) != -1) {
                byte[] chunk = new byte[n];
                System.arraycopy(buf, 0, chunk, 0, n);
                consumer.accept(chunk);
            }
        } finally {
            close();
        }
    }

    /**
     * Connect to a mountpoint and decode RTCM 3.x frames.
     * Calls the consumer for each decoded RTCM frame.
     * Blocks until the connection is closed or an error occurs.
     *
     * @param mountpoint the mountpoint name
     * @param consumer callback receiving decoded RTCM frames
     */
    public void stream(String mountpoint, Consumer<RtcmFrame> consumer) throws IOException {
        stream(mountpoint, null, consumer);
    }

    /**
     * Connect to a mountpoint with GGA position and decode RTCM 3.x frames.
     *
     * @param mountpoint the mountpoint name
     * @param nmeaGga NMEA GGA sentence for VRS, or null
     * @param consumer callback receiving decoded RTCM frames
     */
    public void stream(String mountpoint, String nmeaGga, Consumer<RtcmFrame> consumer)
            throws IOException {
        socket = connect();
        try {
            OutputStream out = socket.getOutputStream();
            out.write(NtripRequest.stream(host, port, mountpoint, version,
                    username, password, nmeaGga));
            out.flush();

            InputStream in = new BufferedInputStream(socket.getInputStream());
            NtripResponse response = NtripResponse.parse(in);

            if (response.isUnauthorized()) {
                throw new IOException("Authentication failed (401). Check credentials.");
            }
            if (response.isSourcetable()) {
                throw new IOException("Mountpoint '" + mountpoint
                        + "' not found. Server returned sourcetable instead.");
            }
            if (!response.isSuccess()) {
                throw new IOException("Stream request failed: " + response.getStatusLine());
            }

            RtcmDecoder decoder = new RtcmDecoder(in);
            decoder.decode(consumer);
        } finally {
            close();
        }
    }

    /**
     * Send an updated NMEA GGA sentence to the caster on an active connection.
     * Used to update position for VRS mountpoints during an active stream.
     */
    public void sendGga(String nmeaGga) throws IOException {
        Socket sock = this.socket;
        if (sock == null || sock.isClosed()) {
            throw new IOException("Not connected");
        }
        OutputStream out = sock.getOutputStream();
        out.write((nmeaGga + "\r\n").getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }

    @Override
    public void close() throws IOException {
        closed = true;
        Socket sock = this.socket;
        if (sock != null && !sock.isClosed()) {
            sock.close();
        }
    }

    private Socket connect() throws IOException {
        Socket sock = new Socket();
        sock.connect(new java.net.InetSocketAddress(host, port), connectTimeoutMs);
        sock.setSoTimeout(readTimeoutMs);
        return sock;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public NtripVersion getVersion() { return version; }

    /**
     * Builder for NtripClient.
     */
    public static class Builder {
        private final String host;
        private final int port;
        private String username;
        private String password;
        private NtripVersion version = NtripVersion.V1;
        private int connectTimeoutMs = 10_000;
        private int readTimeoutMs = 30_000;

        public Builder(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public Builder credentials(String username, String password) {
            this.username = username;
            this.password = password;
            return this;
        }

        public Builder version(NtripVersion version) {
            this.version = version;
            return this;
        }

        public Builder connectTimeout(int ms) {
            this.connectTimeoutMs = ms;
            return this;
        }

        public Builder readTimeout(int ms) {
            this.readTimeoutMs = ms;
            return this;
        }

        public NtripClient build() {
            return new NtripClient(this);
        }
    }
}
