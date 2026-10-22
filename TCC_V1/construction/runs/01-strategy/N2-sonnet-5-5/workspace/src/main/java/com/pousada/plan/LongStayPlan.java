package com.pousada.plan;

import com.pousada.domain.Money;
import com.pousada.error.ApiException;

import java.math.BigDecimal;

public class LongStayPlan implements RatePlan {

    private static final int MINIMUM_NIGHTS = 7;

    @Override
    public String code() {
        return "LONG_STAY";
    }

    @Override
    public void validate(PricingRequest request) {
        if (request.nights() < MINIMUM_NIGHTS) {
            throw ApiException.invalidRequest("A estadia longa exige " + MINIMUM_NIGHTS + " noites ou mais");
        }
    }

    @Override
    public BigDecimal discount(PricingRequest request, BigDecimal subtotal) {
        return Money.percent(subtotal, "0.10");
    }
}
