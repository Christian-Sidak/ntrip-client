package io.github.christiansidak.ntrip.sourcetable;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SourcetableTest {

    private static final String SAMPLE_SOURCETABLE =
            "CAS;rtk2go.com;2101;NtripInfoCaster;BKG;0;USA;37.0;-122.0;0;0;misc\r\n"
            + "NET;RTCM-Ntrip;BKG;B;N;http://example.com;;;\r\n"
            + "STR;CMR_NEAR;CMR_NEAR;RTCM 3.2;1004(1),1005(10),1012(1),1033(30);2;GPS+GLO;SNIP;USA;"
            + "37.7749;-122.4194;1;0;sNTRIP;none;N;N;5200;misc\r\n"
            + "STR;VRS_GPS;My VRS;RTCM 3.3;1074(1),1084(1),1094(1);2;GPS+GLO+GAL;MyNet;DEU;"
            + "50.1109;8.6821;1;1;Trimble;none;B;N;8000;\r\n"
            + "ENDSOURCETABLE\r\n";

    @Test
    void parsesAllEntryTypes() throws IOException {
        Sourcetable table = parse(SAMPLE_SOURCETABLE);

        assertEquals(1, table.getCasters().size());
        assertEquals(1, table.getNetworks().size());
        assertEquals(2, table.getStreams().size());
    }

    @Test
    void parsesCasterEntry() throws IOException {
        Sourcetable table = parse(SAMPLE_SOURCETABLE);
        CasterEntry cas = table.getCasters().get(0);

        assertEquals("rtk2go.com", cas.getHost());
        assertEquals(2101, cas.getPort());
        assertEquals("NtripInfoCaster", cas.getIdentifier());
        assertEquals("USA", cas.getCountry());
    }

    @Test
    void parsesStreamEntry() throws IOException {
        Sourcetable table = parse(SAMPLE_SOURCETABLE);
        StreamEntry str = table.findStream("CMR_NEAR");

        assertNotNull(str);
        assertEquals("CMR_NEAR", str.getMountpoint());
        assertEquals("RTCM 3.2", str.getFormat());
        assertTrue(str.isRtcm3());
        assertEquals("GPS+GLO", str.getNavSystem());
        assertEquals("USA", str.getCountry());
        assertEquals(37.7749, str.getLatitude(), 0.001);
        assertEquals(-122.4194, str.getLongitude(), 0.001);
        assertTrue(str.isNmea());
        assertFalse(str.isFee());
    }

    @Test
    void parsesNetworkEntry() throws IOException {
        Sourcetable table = parse(SAMPLE_SOURCETABLE);
        NetworkEntry net = table.getNetworks().get(0);

        assertEquals("RTCM-Ntrip", net.getIdentifier());
        assertEquals("BKG", net.getOperator());
        assertEquals("B", net.getAuthentication());
    }

    @Test
    void findStreamCaseInsensitive() throws IOException {
        Sourcetable table = parse(SAMPLE_SOURCETABLE);
        assertNotNull(table.findStream("cmr_near"));
        assertNull(table.findStream("nonexistent"));
    }

    @Test
    void getRtcm3Streams() throws IOException {
        Sourcetable table = parse(SAMPLE_SOURCETABLE);
        assertEquals(2, table.getRtcm3Streams().size());
    }

    @Test
    void handlesEmptySourcetable() throws IOException {
        Sourcetable table = parse("ENDSOURCETABLE\r\n");
        assertTrue(table.getEntries().isEmpty());
    }

    private Sourcetable parse(String content) throws IOException {
        return Sourcetable.parse(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }
}
