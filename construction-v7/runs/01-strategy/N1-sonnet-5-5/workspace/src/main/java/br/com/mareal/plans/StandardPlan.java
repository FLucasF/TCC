package br.com.mareal.plans;

import br.com.mareal.domain.Season;
import java.math.BigDecimal;

public class StandardPlan implements RatePlan {
    public String code() {
        return "STANDARD";
    }

    public Pricing price(Stay stay) {
        return Pricing.noDiscount(Season.sumOfNights(stay.roomType(), stay.checkIn(), stay.checkOut()));
    }

    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
