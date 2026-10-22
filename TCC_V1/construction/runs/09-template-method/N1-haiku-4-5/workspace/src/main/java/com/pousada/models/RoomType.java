package com.pousada.models;

public enum RoomType {
    STANDARD("STANDARD"),
    DELUXE("DELUXE"),
    SUITE("SUITE");

    private final String label;

    RoomType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static RoomType fromLabel(String label) {
        for (RoomType type : RoomType.values()) {
            if (type.label.equals(label)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown room type: " + label);
    }
}
