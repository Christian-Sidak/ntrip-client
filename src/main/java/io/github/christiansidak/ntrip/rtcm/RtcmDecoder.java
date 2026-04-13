package io.github.christiansidak.ntrip.rtcm;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

/**
 * Streaming decoder for RTCM 3.x binary frames from an InputStream.
 *
 * Scans for the 0xD3 preamble, reads the length, payload, and CRC,
 * validates the CRC-24Q checksum, and emits complete RtcmFrame objects.
 */
public class RtcmDecoder {

    private static final int PREAMBLE = 0xD3;
    private static final int MAX_PAYLOAD_LENGTH = 1023;
    private static final int HEADER_SIZE = 3;
    private static final int CRC_SIZE = 3;

    private final InputStream input;

    public RtcmDecoder(InputStream input) {
        this.input = input;
    }

    /**
     * Read and decode RTCM frames, passing each to the consumer.
     * Blocks until the stream is closed or an IOException occurs.
     */
    public void decode(Consumer<RtcmFrame> consumer) throws IOException {
        byte[] headerBuf = new byte[HEADER_SIZE];

        while (true) {
            // Scan for preamble
            int b = input.read();
            if (b == -1) return;
            if (b != PREAMBLE) continue;

            headerBuf[0] = (byte) PREAMBLE;

            // Read 2 more header bytes (reserved bits + 10-bit length)
            if (readFully(headerBuf, 1, 2) < 2) return;

            int reserved = (headerBuf[1] >> 2) & 0x3F;
            if (reserved != 0) continue; // reserved bits must be zero

            int payloadLength = ((headerBuf[1] & 0x03) << 8) | (headerBuf[2] & 0xFF);
            if (payloadLength > MAX_PAYLOAD_LENGTH) continue;

            // Read payload + CRC
            byte[] frame = new byte[HEADER_SIZE + payloadLength + CRC_SIZE];
            System.arraycopy(headerBuf, 0, frame, 0, HEADER_SIZE);

            int remaining = payloadLength + CRC_SIZE;
            if (readFully(frame, HEADER_SIZE, remaining) < remaining) return;

            // Validate CRC
            if (!Crc24q.verify(frame, 0, frame.length)) continue;

            // Extract message type (first 12 bits of payload)
            int messageType = 0;
            if (payloadLength >= 2) {
                messageType = ((frame[3] & 0xFF) << 4) | ((frame[4] & 0xF0) >> 4);
            }

            byte[] payload = new byte[payloadLength];
            System.arraycopy(frame, HEADER_SIZE, payload, 0, payloadLength);

            consumer.accept(new RtcmFrame(messageType, payload));
        }
    }

    private int readFully(byte[] buf, int offset, int length) throws IOException {
        int totalRead = 0;
        while (totalRead < length) {
            int n = input.read(buf, offset + totalRead, length - totalRead);
            if (n == -1) return totalRead;
            totalRead += n;
        }
        return totalRead;
    }
}
