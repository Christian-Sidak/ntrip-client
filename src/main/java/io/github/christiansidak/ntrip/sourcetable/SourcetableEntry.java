package io.github.christiansidak.ntrip.sourcetable;

/**
 * Base type for NTRIP sourcetable entries.
 * The sourcetable contains STR (stream), CAS (caster), and NET (network) records.
 */
public abstract class SourcetableEntry {

    public enum Type { STR, CAS, NET }

    private final Type type;

    protected SourcetableEntry(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
