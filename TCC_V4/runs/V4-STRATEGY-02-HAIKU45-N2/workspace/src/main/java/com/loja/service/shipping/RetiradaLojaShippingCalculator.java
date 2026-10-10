package com.loja.service.shipping;

import java.math.BigDecimal;

public class RetiradaLojaShippingCalculator implements ShippingCalculator {
    private static final BigDecimal COST = BigDecimal.ZERO;
    private static final Integer PRAZO = 1;

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
        return true;
    }
}
