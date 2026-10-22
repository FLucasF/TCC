package com.pousadamareaalta.model;

public enum RoomNumber {
    ROOM_101(RoomType.STANDARD),
    ROOM_102(RoomType.STANDARD),
    ROOM_201(RoomType.DELUXE),
    ROOM_202(RoomType.DELUXE),
    ROOM_301(RoomType.SUITE);

    private final RoomType type;

    RoomNumber(RoomType type) {
        this.type = type;
    }

    public RoomType getType() {
        return type;
    }

    public int getNumber() {
        return Integer.parseInt(name().replace("ROOM_", ""));
    }

    public static RoomNumber fromNumber(int number) {
        return valueOf("ROOM_" + number);
    }
}
