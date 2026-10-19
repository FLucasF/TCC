package br.com.mareal.plans;

import br.com.mareal.domain.Money;
import br.com.mareal.domain.Season;
import java.math.BigDecimal;

public class NonRefundablePlan implements RatePlan {
    private static final BigDecimal DISCOUNT = new BigDecimal("0.15");

    public String code() {
        return "NON_REFUNDABLE";
    }

    public Pricing price(Stay stay) {
        return Pricing.withDiscount(Season.sumOfNights(stay.roomType(), stay.checkIn(), stay.checkOut()), DISCOUNT);
    }

    public BigDecimal refund(BigDecimal amountPaid) {
        return Money.ZERO;
    }
}
