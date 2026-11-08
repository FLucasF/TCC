package com.pousada.marealta.enums;

public enum RoomType {
    STANDARD("STANDARD"),
    DELUXE("DELUXE"),
    SUITE("SUITE");

    public final String displayName;

    RoomType(String displayName) {
        this.displayName = displayName;
    }
}
