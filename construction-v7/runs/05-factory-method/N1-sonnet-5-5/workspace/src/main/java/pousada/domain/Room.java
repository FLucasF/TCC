package pousada.domain;

import java.math.BigDecimal;
import java.util.List;

public record Room(int number, RoomType type, int capacity, BigDecimal nightlyRate) {

    public enum RoomType { STANDARD, DELUXE, SUITE }

    public static List<Room> inn() {
        return List.of(
                new Room(101, RoomType.STANDARD, 2, Money.of("249.90")),
                new Room(102, RoomType.STANDARD, 2, Money.of("249.90")),
                new Room(201, RoomType.DELUXE, 3, Money.of("389.90")),
                new Room(202, RoomType.DELUXE, 3, Money.of("389.90")),
                new Room(301, RoomType.SUITE, 4, Money.of("579.90")));
    }
}
