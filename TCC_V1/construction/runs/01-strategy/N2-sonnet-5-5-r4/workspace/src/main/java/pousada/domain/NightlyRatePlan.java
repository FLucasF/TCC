package pousada.domain;

import java.math.BigDecimal;

/** Planos que cobram a soma das noites e aplicam um percentual de desconto sobre ela. */
public abstract class NightlyRatePlan implements RatePlan {
    @Override
    public void validate(Booking booking) {}

    @Override
    public final Quote quote(Booking booking) {
        BigDecimal subtotal = Seasons.sumOfNights(booking.room(), booking.checkIn(), booking.checkOut());
        return new Quote(subtotal, Money.portion(subtotal, discountRate(booking)));
    }

    /** Fração de desconto sobre a soma das noites (0 = sem desconto). */
    protected abstract BigDecimal discountRate(Booking booking);

    protected static BigDecimal fullRefund(BigDecimal amountPaid) {
        return Money.round(amountPaid);
    }
}
