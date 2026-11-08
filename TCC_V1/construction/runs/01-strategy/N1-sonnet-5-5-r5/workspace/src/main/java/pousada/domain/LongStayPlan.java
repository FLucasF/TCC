package pousada.domain;

import java.math.BigDecimal;

/** Estadia de 7 noites ou mais, com 10% de desconto sobre as diárias. */
class LongStayPlan implements RatePlan {
    private static final int MIN_NIGHTS = 7;
    private static final int DISCOUNT_PERCENT = 10;

    @Override
    public String code() {
        return "LONG_STAY";
    }

    @Override
    public void validate(Booking booking) {
        if (booking.nights() < MIN_NIGHTS) {
            throw DomainException.invalidRequest("O plano LONG_STAY exige " + MIN_NIGHTS + " noites ou mais.");
        }
    }

    @Override
    public BigDecimal discount(Booking booking, BigDecimal subtotal) {
        return Money.percent(subtotal, DISCOUNT_PERCENT);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
