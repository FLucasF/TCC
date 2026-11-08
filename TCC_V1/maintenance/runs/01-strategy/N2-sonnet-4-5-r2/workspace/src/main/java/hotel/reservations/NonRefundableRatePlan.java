package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 15% off, but nothing comes back on cancellation. */
public final class NonRefundableRatePlan implements RatePlanStrategy {

    static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        // No special rules for non-refundable stays
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator calculator) {
        BigDecimal subtotal = calculator.calculateSubtotal(room, checkIn, checkOut);
        int nights = calculator.countNights(checkIn, checkOut);
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(DISCOUNT_RATE));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }
}
