package br.marealta.model;

public enum RoomType {
    STANDARD(2),
    DELUXE(3),
    SUITE(4);

    private final int capacity;

    RoomType(int capacity) {
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }
}
