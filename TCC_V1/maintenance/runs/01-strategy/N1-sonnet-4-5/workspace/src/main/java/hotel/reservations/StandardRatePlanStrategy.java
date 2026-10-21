package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Full price; the guest gets everything back when cancelling a confirmed reservation. */
final class StandardRatePlanStrategy implements RatePlanStrategy {

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        // no restrictions
    }

    @Override
    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        BigDecimal subtotal = PriceCalculator.sumNights(room, checkIn, checkOut);
        BigDecimal discount = PriceCalculator.cents(BigDecimal.ZERO);
        return new Price(
                PriceCalculator.countNights(checkIn, checkOut), subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal refundAmount(BigDecimal amountPaid) {
        return amountPaid;
    }
}
