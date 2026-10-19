package br.com.marealta;

import java.math.BigDecimal;
import java.util.List;

public record Room(int number, RoomType type) {

    public static final List<Room> ALL = List.of(
            new Room(101, RoomType.STANDARD),
            new Room(102, RoomType.STANDARD),
            new Room(201, RoomType.DELUXE),
            new Room(202, RoomType.DELUXE),
            new Room(301, RoomType.SUITE));

    public int capacity() { return type.capacity(); }

    public BigDecimal nightlyRate() { return type.nightlyRate(); }
}
