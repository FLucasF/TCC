package com.loja.service.shipping;

import com.loja.util.MoneyRounder;
import java.math.BigDecimal;

public class ExpressaShippingCalculator implements ShippingCalculator {
    private static final BigDecimal BASE_COST = new BigDecimal("25.00");
    private static final BigDecimal COST_PER_KG = new BigDecimal("4.50");
    private static final Integer PRAZO = 2;

    @Override
    public BigDecimal calculate(BigDecimal totalWeightKg) {
        BigDecimal weightCost = totalWeightKg.multiply(COST_PER_KG);
        BigDecimal total = BASE_COST.add(weightCost);
        return MoneyRounder.round(total);
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
