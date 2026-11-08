package br.com.marealta;

import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;
import java.math.BigDecimal;
import java.util.List;

final class Config {
    private Config() {}

    static int port() {
        String port = System.getenv("PORT");
        return port == null || port.isBlank() ? 7070 : Integer.parseInt(port.trim());
    }

    static List<Room> initialRooms() {
        return List.of(
                new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90")),
                new Room(102, RoomType.STANDARD, 2, new BigDecimal("249.90")),
                new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90")),
                new Room(202, RoomType.DELUXE, 3, new BigDecimal("389.90")),
                new Room(301, RoomType.SUITE, 4, new BigDecimal("579.90")));
    }
}
