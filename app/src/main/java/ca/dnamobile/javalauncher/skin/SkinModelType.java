package ca.dnamobile.javalauncher.skin;

public enum SkinModelType {
    CLASSIC("classic"),
    SLIM("slim");

    public final String id;

    SkinModelType(String id) { this.id = id; }

    public static SkinModelType fromId(String id) {
        for (SkinModelType t : values()) { if (t.id.equalsIgnoreCase(id)) return t; }
        return CLASSIC;
    }
}
