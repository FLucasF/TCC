package hotel.reservations;

import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.util.Map;

public final class WeekendPackagePrice {

    private static final Map<RoomType, BigDecimal> PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    public static BigDecimal priceFor(RoomType roomType) {
        return PRICES.get(roomType);
    }
}
