package com.marealta;

public record Room(int number, RoomType type) {
    public int capacity() { return type.capacity(); }
}
