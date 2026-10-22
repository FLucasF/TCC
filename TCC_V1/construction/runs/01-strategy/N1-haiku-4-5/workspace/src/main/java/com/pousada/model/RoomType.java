package com.pousada.model;

public enum RoomType {
    STANDARD("STANDARD"),
    DELUXE("DELUXE"),
    SUITE("SUITE");

    private final String displayName;

    RoomType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static RoomType fromString(String type) {
        return RoomType.valueOf(type.toUpperCase());
    }
}
