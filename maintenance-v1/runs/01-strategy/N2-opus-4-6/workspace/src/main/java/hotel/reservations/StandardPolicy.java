package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

final class StandardPolicy implements RatePlanPolicy {

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        int nights = PriceCalculator.countNights(checkIn, checkOut);
        BigDecimal subtotal = PriceCalculator.subtotal(room, checkIn, checkOut);
        BigDecimal discount = PriceCalculator.cents(BigDecimal.ZERO);
        return new Price(nights, subtotal, discount, subtotal);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
