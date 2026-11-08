package br.com.mare.plan;

import br.com.mare.domain.Money;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Planos cobrados por diária: soma das noites, menos um desconto percentual. */
abstract class NightlyRatePlan implements RatePlan {
    protected static final BigDecimal NONE = BigDecimal.ZERO;
    protected static final BigDecimal ALL = BigDecimal.ONE;

    @Override
    public final Quote quote(Booking booking) {
        validate(booking);
        BigDecimal subtotal = Money.ZERO;
        for (LocalDate night = booking.checkIn(); night.isBefore(booking.checkOut()); night = night.plusDays(1)) {
            subtotal = Money.round(subtotal.add(booking.room().priceOn(night)));
        }
        BigDecimal discount = Money.round(subtotal.multiply(discountRate(booking)));
        return new Quote(booking.nights(), subtotal, discount, Money.round(subtotal.subtract(discount)));
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return Money.round(amountPaid.multiply(refundRate()));
    }

    protected void validate(Booking booking) {}

    protected BigDecimal discountRate(Booking booking) { return NONE; }

    protected BigDecimal refundRate() { return ALL; }
}
