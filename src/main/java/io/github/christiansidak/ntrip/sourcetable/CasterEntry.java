package io.github.christiansidak.ntrip.sourcetable;

/**
 * A CAS (caster) entry from the NTRIP sourcetable.
 *
 * Format: CAS;host;port;identifier;operator;nmea;country;latitude;longitude;fallback-host;fallback-port;misc
 */
public class CasterEntry extends SourcetableEntry {

    private final String host;
    private final int port;
    private final String identifier;
    private final String operator;
    private final boolean nmea;
    private final String country;
    private final double latitude;
    private final double longitude;

    CasterEntry(String[] fields) {
        super(Type.CAS);
        this.host = field(fields, 1);
        this.port = intField(fields, 2);
        this.identifier = field(fields, 3);
        this.operator = field(fields, 4);
        this.nmea = "1".equals(field(fields, 5));
        this.country = field(fields, 6);
        this.latitude = doubleField(fields, 7);
        this.longitude = doubleField(fields, 8);
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

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getIdentifier() { return identifier; }
    public String getOperator() { return operator; }
    public boolean isNmea() { return nmea; }
    public String getCountry() { return country; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    @Override
    public String toString() {
        return "CAS:" + host + ":" + port + " (" + identifier + ")";
    }
}
