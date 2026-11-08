package com.pousada.marealta.domain.room;

public enum RoomType {
    STANDARD("STANDARD", 2),
    DELUXE("DELUXE", 3),
    SUITE("SUITE", 4);

    private final String name;
    private final int capacity;

    RoomType(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;
    }

    public String displayName() {
        return name;
    }

    public int capacity() {
        return capacity;
    }

    public static RoomType fromString(String value) {
        for (RoomType type : RoomType.values()) {
            if (type.name.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown room type: " + value);
    }
}
