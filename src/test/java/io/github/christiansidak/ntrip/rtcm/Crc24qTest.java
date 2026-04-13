package io.github.christiansidak.ntrip.rtcm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Crc24qTest {

    @Test
    void computeKnownValue() {
        // "123456789" is a standard CRC test vector
        byte[] data = "123456789".getBytes();
        int crc = Crc24q.compute(data, 0, data.length);
        // CRC-24Q("123456789") = 0xCDE703
        assertEquals(0xCDE703, crc);
    }

    @Test
    void verifyValidFrame() {
        // Build a minimal frame: preamble + length + 2-byte payload + CRC
        byte[] payload = {0x00, 0x10}; // message type 1 (shifted)
        byte[] header = {(byte) 0xD3, 0x00, 0x02};
        byte[] frameWithoutCrc = new byte[header.length + payload.length];
        System.arraycopy(header, 0, frameWithoutCrc, 0, header.length);
        System.arraycopy(payload, 0, frameWithoutCrc, header.length, payload.length);

        int crc = Crc24q.compute(frameWithoutCrc, 0, frameWithoutCrc.length);

        byte[] frame = new byte[frameWithoutCrc.length + 3];
        System.arraycopy(frameWithoutCrc, 0, frame, 0, frameWithoutCrc.length);
        frame[frame.length - 3] = (byte) ((crc >> 16) & 0xFF);
        frame[frame.length - 2] = (byte) ((crc >> 8) & 0xFF);
        frame[frame.length - 1] = (byte) (crc & 0xFF);

        assertTrue(Crc24q.verify(frame, 0, frame.length));
    }

    @Test
    void verifyCorruptedFrame() {
        byte[] frame = {(byte) 0xD3, 0x00, 0x02, 0x00, 0x10, 0x00, 0x00, 0x00};
        assertFalse(Crc24q.verify(frame, 0, frame.length));
    }

    @Test
    void emptyInput() {
        assertEquals(0, Crc24q.compute(new byte[0], 0, 0));
    }
}
