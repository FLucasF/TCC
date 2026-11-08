package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

final class NonRefundableRatePlan extends StandardRatePlan {

    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    @Override
    public Price calculate(Room room, ReservationData data) {
        Price standardPrice = super.calculate(room, data);
        BigDecimal discount = cents(standardPrice.subtotal().multiply(NON_REFUNDABLE_DISCOUNT));
        return new Price(
                standardPrice.nights(),
                standardPrice.subtotal(),
                discount,
                standardPrice.subtotal().subtract(discount));
    }

    @Override
    public BigDecimal refundOnCancellation(Price price) {
        return cents(BigDecimal.ZERO);
    }
}
