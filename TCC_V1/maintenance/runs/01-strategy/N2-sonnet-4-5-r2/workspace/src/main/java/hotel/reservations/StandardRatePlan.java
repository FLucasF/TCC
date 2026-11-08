package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Full price; the guest gets everything back when cancelling a confirmed reservation. */
public final class StandardRatePlan implements RatePlanStrategy {

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        // No special rules for standard stays
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator calculator) {
        BigDecimal subtotal = calculator.calculateSubtotal(room, checkIn, checkOut);
        int nights = calculator.countNights(checkIn, checkOut);
        BigDecimal discount = PriceCalculator.cents(BigDecimal.ZERO);
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
