package com.pousada;

public enum RoomType {
    STANDARD("STANDARD"),
    DELUXE("DELUXE"),
    SUITE("SUITE");

    public final String displayName;

    RoomType(String displayName) {
        this.displayName = displayName;
    }

    public static RoomType fromString(String value) {
        return RoomType.valueOf(value.toUpperCase());
    }
}
