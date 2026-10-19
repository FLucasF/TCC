package marealta;

import java.math.BigDecimal;

public record RoomView(int number, RoomType type, int capacity, BigDecimal nightlyRate) {
    public static RoomView of(Room r) {
        return new RoomView(r.number(), r.type(), r.capacity(), r.nightlyRate());
    }
}
