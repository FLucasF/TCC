package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Full price; the guest gets everything back when cancelling a confirmed reservation. */
final class StandardPolicy implements RatePlanPolicy {

    @Override
    public Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator pricing) {
        BigDecimal subtotal = pricing.nightsSubtotal(room, checkIn, checkOut);
        return new Price(PriceCalculator.nights(checkIn, checkOut), subtotal, PriceCalculator.cents(BigDecimal.ZERO), subtotal);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
