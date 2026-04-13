package io.github.christiansidak.ntrip.sourcetable;

/**
 * A NET (network) entry from the NTRIP sourcetable.
 *
 * Format: NET;identifier;operator;authentication;fee;web-net;web-str;web-reg;misc
 */
public class NetworkEntry extends SourcetableEntry {

    private final String identifier;
    private final String operator;
    private final String authentication;
    private final boolean fee;
    private final String webNet;
    private final String webStr;
    private final String webReg;

    NetworkEntry(String[] fields) {
        super(Type.NET);
        this.identifier = field(fields, 1);
        this.operator = field(fields, 2);
        this.authentication = field(fields, 3);
        this.fee = "Y".equalsIgnoreCase(field(fields, 4));
        this.webNet = field(fields, 5);
        this.webStr = field(fields, 6);
        this.webReg = field(fields, 7);
    }

    private static String field(String[] fields, int index) {
        return index < fields.length ? fields[index].trim() : "";
    }

    public String getIdentifier() { return identifier; }
    public String getOperator() { return operator; }
    public String getAuthentication() { return authentication; }
    public boolean isFee() { return fee; }
    public String getWebNet() { return webNet; }
    public String getWebStr() { return webStr; }
    public String getWebReg() { return webReg; }

    @Override
    public String toString() {
        return "NET:" + identifier + " (" + operator + ")";
    }
}
