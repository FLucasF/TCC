package br.com.mare.domain;

import java.util.List;
import java.util.Optional;

/** Os quartos da pousada. Quase nunca mudam, por isso ficam no código. */
public final class Hotel {
    private static final List<Room> ROOMS = List.of(
            new Room(101, RoomType.STANDARD),
            new Room(102, RoomType.STANDARD),
            new Room(201, RoomType.DELUXE),
            new Room(202, RoomType.DELUXE),
            new Room(301, RoomType.SUITE));

    private Hotel() {
    }

    public static List<Room> rooms() {
        return ROOMS;
    }

    public static Optional<Room> room(int number) {
        return ROOMS.stream().filter(r -> r.number() == number).findFirst();
    }

    public static Room requireRoom(int number) {
        return room(number).orElseThrow(() ->
                new BusinessException(ErrorCode.ROOM_NOT_FOUND, "Quarto " + number + " não existe"));
    }
}
