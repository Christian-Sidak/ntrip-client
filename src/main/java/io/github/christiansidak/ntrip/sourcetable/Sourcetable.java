package io.github.christiansidak.ntrip.sourcetable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parsed NTRIP caster sourcetable.
 */
public class Sourcetable {

    private final List<SourcetableEntry> entries;

    private Sourcetable(List<SourcetableEntry> entries) {
        this.entries = Collections.unmodifiableList(entries);
    }

    /**
     * Parse a sourcetable from the response body stream.
     */
    public static Sourcetable parse(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        List<SourcetableEntry> entries = new ArrayList<>();

        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.equals("ENDSOURCETABLE")) {
                continue;
            }

            String[] fields = line.split(";", -1);
            if (fields.length < 2) continue;

            String type = fields[0].trim().toUpperCase();
            switch (type) {
                case "STR":
                    entries.add(new StreamEntry(fields));
                    break;
                case "CAS":
                    entries.add(new CasterEntry(fields));
                    break;
                case "NET":
                    entries.add(new NetworkEntry(fields));
                    break;
                default:
                    break;
            }
        }

        return new Sourcetable(entries);
    }

    public List<SourcetableEntry> getEntries() {
        return entries;
    }

    public List<StreamEntry> getStreams() {
        return entries.stream()
                .filter(e -> e instanceof StreamEntry)
                .map(e -> (StreamEntry) e)
                .collect(Collectors.toList());
    }

    public List<CasterEntry> getCasters() {
        return entries.stream()
                .filter(e -> e instanceof CasterEntry)
                .map(e -> (CasterEntry) e)
                .collect(Collectors.toList());
    }

    public List<NetworkEntry> getNetworks() {
        return entries.stream()
                .filter(e -> e instanceof NetworkEntry)
                .map(e -> (NetworkEntry) e)
                .collect(Collectors.toList());
    }

    /**
     * Find a stream entry by mountpoint name (case-insensitive).
     */
    public StreamEntry findStream(String mountpoint) {
        return getStreams().stream()
                .filter(s -> s.getMountpoint().equalsIgnoreCase(mountpoint))
                .findFirst()
                .orElse(null);
    }

    /**
     * Find all RTCM 3.x streams.
     */
    public List<StreamEntry> getRtcm3Streams() {
        return getStreams().stream()
                .filter(StreamEntry::isRtcm3)
                .collect(Collectors.toList());
    }
}
