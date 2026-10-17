package hotel.reservations;

import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.util.Optional;

public final class WeekendPackagePrice {

    private static final BigDecimal STANDARD_PRICE = new BigDecimal("520.00");
    private static final BigDecimal DELUXE_PRICE = new BigDecimal("820.00");
    private static final BigDecimal SUITE_PRICE = new BigDecimal("1200.00");

    static Optional<BigDecimal> priceFor(RoomType roomType) {
        return switch (roomType) {
            case STANDARD -> Optional.of(STANDARD_PRICE);
            case DELUXE -> Optional.of(DELUXE_PRICE);
            case SUITE -> Optional.of(SUITE_PRICE);
        };
    }
}
