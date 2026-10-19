package com.pousadamarealta.domain;

public enum RoomType {
    STANDARD("STANDARD"),
    DELUXE("DELUXE"),
    SUITE("SUITE");

    private final String code;

    RoomType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
