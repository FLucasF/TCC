package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Full price; everything comes back on cancellation. */
final class StandardRule implements RatePlanRule {

    @Override
    public Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, Price standardPrice) {
        return standardPrice;
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
