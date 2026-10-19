package com.pousadamareal.reservas.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RoomType {
    STANDARD("STANDARD"),
    DELUXE("DELUXE"),
    SUITE("SUITE");

    private final String value;

    RoomType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static RoomType fromString(String value) {
        for (RoomType type : RoomType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid RoomType: " + value);
    }
}
