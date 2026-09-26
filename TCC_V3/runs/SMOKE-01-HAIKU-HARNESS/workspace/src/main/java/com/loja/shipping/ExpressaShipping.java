package com.loja.shipping;

import java.math.BigDecimal;

public class ExpressaShipping implements ShippingMethod {
    private static final BigDecimal BASE_COST = new BigDecimal("25.00");
    private static final BigDecimal COST_PER_KG = new BigDecimal("4.50");
    private static final Integer DELIVERY_DAYS = 2;

    @Override
    public BigDecimal calculateCost(BigDecimal totalWeightKg) {
        return BASE_COST.add(COST_PER_KG.multiply(totalWeightKg));
    }

    @Override
    public Integer getDeliveryDays() {
        return DELIVERY_DAYS;
    }

    @Override
    public boolean isAvailable(BigDecimal totalWeightKg) {
        return true;
    }

    @Override
    public String getCode() {
        return "EXPRESSA";
    }
}
