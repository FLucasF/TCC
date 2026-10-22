package com.pousada.domain;

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
}
