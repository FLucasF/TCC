package br.com.pousadamarealta;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public record Room(int number, RoomType type, int capacity, BigDecimal nightlyRate) {

    public enum RoomType { STANDARD, DELUXE, SUITE }

    public static final List<Room> ALL = List.of(
            new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90")),
            new Room(102, RoomType.STANDARD, 2, new BigDecimal("249.90")),
            new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90")),
            new Room(202, RoomType.DELUXE, 3, new BigDecimal("389.90")),
            new Room(301, RoomType.SUITE, 4, new BigDecimal("579.90")));

    public static Optional<Room> find(int number) {
        return ALL.stream().filter(r -> r.number() == number).findFirst();
    }
}
