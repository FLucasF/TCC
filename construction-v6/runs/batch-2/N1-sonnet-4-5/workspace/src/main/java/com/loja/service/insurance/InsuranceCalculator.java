package com.loja.service.insurance;

import com.loja.domain.Region;
import com.loja.util.MoneyUtils;

import java.math.BigDecimal;

public class InsuranceCalculator {
    public BigDecimal calculate(BigDecimal productsSubtotal, Region region) {
        BigDecimal percentage;
        BigDecimal limit;

        switch (region) {
            case SOUTHEAST, SOUTH -> {
                percentage = new BigDecimal("0.01");
                limit = null;
            }
            case MIDWEST -> {
                percentage = new BigDecimal("0.015");
                limit = null;
            }
            case NORTH -> {
                percentage = new BigDecimal("0.025");
                limit = new BigDecimal("30.00");
            }
            case NORTHEAST -> {
                percentage = new BigDecimal("0.02");
                limit = new BigDecimal("30.00");
            }
            default -> {
                percentage = BigDecimal.ZERO;
                limit = null;
            }
        }

        BigDecimal insurance = MoneyUtils.roundMoney(productsSubtotal.multiply(percentage));

        if (limit != null && insurance.compareTo(limit) > 0) {
            return limit;
        }

        return insurance;
    }
}
