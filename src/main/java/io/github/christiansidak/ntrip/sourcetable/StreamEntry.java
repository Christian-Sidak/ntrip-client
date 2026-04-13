package io.github.christiansidak.ntrip.sourcetable;

/**
 * A STR (stream) entry from the NTRIP sourcetable.
 *
 * Format: STR;mountpoint;identifier;format;format-details;carrier;nav-system;
 *         network;country;latitude;longitude;nmea;solution;generator;
 *         compression;authentication;fee;bitrate;misc
 */
public class StreamEntry extends SourcetableEntry {

    private final String mountpoint;
    private final String identifier;
    private final String format;
    private final String formatDetails;
    private final int carrier;
    private final String navSystem;
    private final String network;
    private final String country;
    private final double latitude;
    private final double longitude;
    private final boolean nmea;
    private final boolean solution;
    private final String generator;
    private final String compression;
    private final String authentication;
    private final boolean fee;
    private final int bitrate;
    private final String misc;

    StreamEntry(String[] fields) {
        super(Type.STR);
        this.mountpoint = field(fields, 1);
        this.identifier = field(fields, 2);
        this.format = field(fields, 3);
        this.formatDetails = field(fields, 4);
        this.carrier = intField(fields, 5);
        this.navSystem = field(fields, 6);
        this.network = field(fields, 7);
        this.country = field(fields, 8);
        this.latitude = doubleField(fields, 9);
        this.longitude = doubleField(fields, 10);
        this.nmea = "1".equals(field(fields, 11));
        this.solution = "1".equals(field(fields, 12));
        this.generator = field(fields, 13);
        this.compression = field(fields, 14);
        this.authentication = field(fields, 15);
        this.fee = "Y".equalsIgnoreCase(field(fields, 16));
        this.bitrate = intField(fields, 17);
        this.misc = field(fields, 18);
    }

    private static String field(String[] fields, int index) {
        return index < fields.length ? fields[index].trim() : "";
    }

    private static int intField(String[] fields, int index) {
        String v = field(fields, index);
        if (v.isEmpty()) return 0;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return 0; }
    }

    private static double doubleField(String[] fields, int index) {
        String v = field(fields, index);
        if (v.isEmpty()) return 0.0;
        try { return Double.parseDouble(v); } catch (NumberFormatException e) { return 0.0; }
    }

    public String getMountpoint() { return mountpoint; }
    public String getIdentifier() { return identifier; }
    public String getFormat() { return format; }
    public String getFormatDetails() { return formatDetails; }
    public int getCarrier() { return carrier; }
    public String getNavSystem() { return navSystem; }
    public String getNetwork() { return network; }
    public String getCountry() { return country; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public boolean isNmea() { return nmea; }
    public boolean isSolution() { return solution; }
    public String getGenerator() { return generator; }
    public String getCompression() { return compression; }
    public String getAuthentication() { return authentication; }
    public boolean isFee() { return fee; }
    public int getBitrate() { return bitrate; }
    public String getMisc() { return misc; }

    /**
     * Whether this is an RTCM 3.x stream.
     */
    public boolean isRtcm3() {
        return format.startsWith("RTCM 3") || format.startsWith("RTCM3");
    }

    @Override
    public String toString() {
        return "STR:" + mountpoint + " (" + format + ", " + navSystem + ", " + country + ")";
    }
}
