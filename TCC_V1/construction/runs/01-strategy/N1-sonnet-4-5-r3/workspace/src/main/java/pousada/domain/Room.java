package pousada.domain;

public class Room {
    private final String number;
    private final RoomType type;

    public Room(String number, RoomType type) {
        this.number = number;
        this.type = type;
    }

    public String number() {
        return number;
    }

    public RoomType type() {
        return type;
    }

    public int capacity() {
        return type.capacity();
    }

    public static RoomType typeFromNumber(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> RoomType.STANDARD;
            case "201", "202" -> RoomType.DELUXE;
            case "301" -> RoomType.SUITE;
            default -> throw new IllegalArgumentException("Unknown room number: " + roomNumber);
        };
    }
}
