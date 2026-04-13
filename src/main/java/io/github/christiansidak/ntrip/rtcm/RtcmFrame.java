package io.github.christiansidak.ntrip.rtcm;

/**
 * A single RTCM 3.x message frame.
 *
 * Binary format:
 *   - Preamble: 0xD3 (1 byte)
 *   - Reserved + Length: 6 reserved bits + 10-bit message length (2 bytes)
 *   - Payload: variable length (0-1023 bytes)
 *   - CRC-24Q: 3 bytes
 */
public class RtcmFrame {

    private final int messageType;
    private final byte[] payload;

    public RtcmFrame(int messageType, byte[] payload) {
        this.messageType = messageType;
        this.payload = payload;
    }

    /**
     * The 12-bit RTCM message type number (first 12 bits of payload).
     * Common types:
     *   1004 - GPS L1/L2 observations
     *   1005 - Station coordinates (ARP)
     *   1006 - Station coordinates + height
     *   1012 - GLONASS L1/L2 observations
     *   1033 - Receiver/antenna descriptors
     *   1074-1077 - GPS MSM4-MSM7
     *   1084-1087 - GLONASS MSM4-MSM7
     *   1094-1097 - Galileo MSM4-MSM7
     *   1124-1127 - BeiDou MSM4-MSM7
     *   4072 - Reference station (u-blox proprietary)
     */
    public int getMessageType() {
        return messageType;
    }

    /**
     * Raw payload bytes (excluding preamble, length header, and CRC).
     */
    public byte[] getPayload() {
        return payload;
    }

    /**
     * Total frame size: 3 (header) + payload length + 3 (CRC).
     */
    public int getFrameSize() {
        return 3 + payload.length + 3;
    }

    @Override
    public String toString() {
        return "RTCM(" + messageType + ", " + payload.length + " bytes)";
    }
}
