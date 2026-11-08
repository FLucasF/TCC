package com.marealta.plan;

import com.marealta.Money;
import com.marealta.Seasons;
import java.math.BigDecimal;

/** Plano que cobra as diárias (com alta temporada) e aplica um percentual de desconto sobre a soma. */
abstract class PerNightPlan implements RatePlan {
    /** Percentual de desconto (ex.: "0.15"); lança exceção se o plano não puder ser usado. */
    protected String discountRate(BookingTerms terms) {
        return "0";
    }

    @Override
    public Quote quote(BookingTerms t) {
        String rate = discountRate(t);
        BigDecimal subtotal = Seasons.sumNights(t.room(), t.checkIn(), t.checkOut());
        BigDecimal discount = Money.percent(subtotal, rate);
        return new Quote(t.nights(), subtotal, discount, Money.round(subtotal.subtract(discount)));
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return Money.round(amountPaid);
    }
}
