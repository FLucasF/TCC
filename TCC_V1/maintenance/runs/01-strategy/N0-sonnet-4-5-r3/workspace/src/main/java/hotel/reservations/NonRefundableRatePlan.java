package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

import static hotel.reservations.PriceCalculator.cents;

/** 15% off, but nothing comes back on cancellation. */
final class NonRefundableRatePlan extends BaseRatePlanStrategy {

    private static final BigDecimal DISCOUNT_PERCENTAGE = new BigDecimal("0.15");

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        int nights = countNights(checkIn, checkOut);
        BigDecimal subtotal = calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = cents(subtotal.multiply(DISCOUNT_PERCENTAGE));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return cents(BigDecimal.ZERO);
    }
}
