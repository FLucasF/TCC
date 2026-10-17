package hotel.reservations.plans;

import hotel.reservations.NewReservation;
import hotel.reservations.Price;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** 15% off on the nights; nothing comes back on cancellation. */
public final class NonRefundablePolicy implements RatePlanPolicy {

    private static final BigDecimal DISCOUNT = new BigDecimal("0.15");
    private static final BigDecimal NO_REFUND = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

    @Override
    public void validate(NewReservation request) {
    }

    @Override
    public Price price(Room room, NewReservation request, PricingSupport pricing) {
        int nights = pricing.nights(request.checkIn(), request.checkOut());
        BigDecimal subtotal = pricing.subtotal(room, request.checkIn(), request.checkOut());
        BigDecimal discount = pricing.cents(subtotal.multiply(DISCOUNT));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return NO_REFUND;
    }
}
