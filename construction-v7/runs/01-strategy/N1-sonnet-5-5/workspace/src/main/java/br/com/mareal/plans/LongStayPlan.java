package br.com.mareal.plans;

import br.com.mareal.domain.ApiException;
import br.com.mareal.domain.Season;
import java.math.BigDecimal;

public class LongStayPlan implements RatePlan {
    private static final int MIN_NIGHTS = 7;
    private static final BigDecimal DISCOUNT = new BigDecimal("0.10");

    public String code() {
        return "LONG_STAY";
    }

    public Pricing price(Stay stay) {
        if (stay.nights() < MIN_NIGHTS) {
            throw ApiException.invalid("O plano LONG_STAY exige pelo menos " + MIN_NIGHTS + " noites.");
        }
        return Pricing.withDiscount(Season.sumOfNights(stay.roomType(), stay.checkIn(), stay.checkOut()), DISCOUNT);
    }

    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
