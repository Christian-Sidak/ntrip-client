package io.github.christiansidak.ntrip.rtcm;

/**
 * CRC-24Q implementation for RTCM 3.x frame validation.
 * Polynomial: x^24 + x^23 + x^18 + x^17 + x^14 + x^11 + x^10 + x^7 + x^6 + x^5 + x^4 + x^3 + x + 1
 * (0x1864CFB)
 */
public final class Crc24q {

    private static final int POLYNOMIAL = 0x1864CFB;
    private static final int[] TABLE = new int[256];

    static {
        for (int i = 0; i < 256; i++) {
            int crc = i << 16;
            for (int j = 0; j < 8; j++) {
                crc <<= 1;
                if ((crc & 0x1000000) != 0) {
                    crc ^= POLYNOMIAL;
                }
            }
            TABLE[i] = crc & 0xFFFFFF;
        }
    }

    private Crc24q() {}

    /**
     * Compute CRC-24Q over the given bytes.
     */
    public static int compute(byte[] data, int offset, int length) {
        int crc = 0;
        for (int i = offset; i < offset + length; i++) {
            crc = ((crc << 8) & 0xFFFFFF) ^ TABLE[((crc >> 16) ^ (data[i] & 0xFF)) & 0xFF];
        }
        return crc;
    }

    /**
     * Verify a complete RTCM frame (header + payload + CRC).
     * The CRC of the entire frame (including the 3 CRC bytes) should be zero.
     */
    public static boolean verify(byte[] frame, int offset, int length) {
        return compute(frame, offset, length) == 0;
    }
}
