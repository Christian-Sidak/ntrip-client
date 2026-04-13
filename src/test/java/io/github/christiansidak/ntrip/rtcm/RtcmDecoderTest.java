package io.github.christiansidak.ntrip.rtcm;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RtcmDecoderTest {

    @Test
    void decodesSingleFrame() throws IOException {
        byte[] frame = buildValidFrame(1005, new byte[]{0x00, 0x10, 0x20, 0x30});
        ByteArrayInputStream in = new ByteArrayInputStream(frame);

        RtcmDecoder decoder = new RtcmDecoder(in);
        List<RtcmFrame> frames = new ArrayList<>();
        decoder.decode(frames::add);

        assertEquals(1, frames.size());
        assertEquals(1005, frames.get(0).getMessageType());
    }

    @Test
    void decodesMultipleFrames() throws IOException {
        byte[] frame1 = buildValidFrame(1005, new byte[]{0x00, 0x10});
        byte[] frame2 = buildValidFrame(1077, new byte[]{0x00, 0x20, 0x30});

        byte[] combined = new byte[frame1.length + frame2.length];
        System.arraycopy(frame1, 0, combined, 0, frame1.length);
        System.arraycopy(frame2, 0, combined, frame1.length, frame2.length);

        ByteArrayInputStream in = new ByteArrayInputStream(combined);
        RtcmDecoder decoder = new RtcmDecoder(in);
        List<RtcmFrame> frames = new ArrayList<>();
        decoder.decode(frames::add);

        assertEquals(2, frames.size());
    }

    @Test
    void skipsGarbageBeforeFrame() throws IOException {
        byte[] validFrame = buildValidFrame(1005, new byte[]{0x00, 0x10});
        byte[] garbage = {0x01, 0x02, 0x03, 0x04, 0x05};

        byte[] combined = new byte[garbage.length + validFrame.length];
        System.arraycopy(garbage, 0, combined, 0, garbage.length);
        System.arraycopy(validFrame, 0, combined, garbage.length, validFrame.length);

        ByteArrayInputStream in = new ByteArrayInputStream(combined);
        RtcmDecoder decoder = new RtcmDecoder(in);
        List<RtcmFrame> frames = new ArrayList<>();
        decoder.decode(frames::add);

        assertEquals(1, frames.size());
    }

    @Test
    void handlesEmptyStream() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        RtcmDecoder decoder = new RtcmDecoder(in);
        List<RtcmFrame> frames = new ArrayList<>();
        decoder.decode(frames::add);
        assertTrue(frames.isEmpty());
    }

    /**
     * Build a valid RTCM frame with correct CRC for testing.
     * The message type is encoded into the first 12 bits of the payload.
     */
    private byte[] buildValidFrame(int messageType, byte[] extraPayload) {
        // Encode message type into first 2 bytes
        byte[] payload = new byte[2 + extraPayload.length];
        payload[0] = (byte) ((messageType >> 4) & 0xFF);
        payload[1] = (byte) (((messageType & 0x0F) << 4) | (extraPayload.length > 0 ? (extraPayload[0] & 0x0F) : 0));
        if (extraPayload.length > 0) {
            System.arraycopy(extraPayload, 1, payload, 2, extraPayload.length - 1);
            // Adjust: we used first nibble of extraPayload[0] above
        }
        // Simpler: just put message type in first 12 bits cleanly
        payload[0] = (byte) ((messageType >> 4) & 0xFF);
        payload[1] = (byte) ((messageType & 0x0F) << 4);
        // Append extra payload after the 2 message-type bytes
        byte[] fullPayload = new byte[2 + extraPayload.length];
        fullPayload[0] = payload[0];
        fullPayload[1] = payload[1];
        System.arraycopy(extraPayload, 0, fullPayload, 2, extraPayload.length);

        int len = fullPayload.length;
        byte[] frameWithoutCrc = new byte[3 + len];
        frameWithoutCrc[0] = (byte) 0xD3;
        frameWithoutCrc[1] = (byte) ((len >> 8) & 0x03);
        frameWithoutCrc[2] = (byte) (len & 0xFF);
        System.arraycopy(fullPayload, 0, frameWithoutCrc, 3, len);

        int crc = Crc24q.compute(frameWithoutCrc, 0, frameWithoutCrc.length);
        byte[] frame = new byte[frameWithoutCrc.length + 3];
        System.arraycopy(frameWithoutCrc, 0, frame, 0, frameWithoutCrc.length);
        frame[frame.length - 3] = (byte) ((crc >> 16) & 0xFF);
        frame[frame.length - 2] = (byte) ((crc >> 8) & 0xFF);
        frame[frame.length - 1] = (byte) (crc & 0xFF);

        return frame;
    }
}
