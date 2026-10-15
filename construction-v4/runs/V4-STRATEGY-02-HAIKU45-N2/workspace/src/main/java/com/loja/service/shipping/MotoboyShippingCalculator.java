package com.loja.service.shipping;

import java.math.BigDecimal;

public class MotoboyShippingCalculator implements ShippingCalculator {
    private static final BigDecimal COST = new BigDecimal("18.00");
    private static final Integer PRAZO = 0;
    private static final BigDecimal MAX_WEIGHT = new BigDecimal("5.00");

    @Override
    public BigDecimal calculate(BigDecimal totalWeightKg) {
        return COST;
    }

    @Override
    public Integer getPrazo() {
        return PRAZO;
    }

    @Override
    public boolean isAvailable(BigDecimal totalWeightKg) {
        return totalWeightKg.compareTo(MAX_WEIGHT) <= 0;
    }
}
