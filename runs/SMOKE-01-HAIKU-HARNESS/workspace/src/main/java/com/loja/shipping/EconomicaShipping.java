package com.loja.shipping;

import java.math.BigDecimal;

public class EconomicaShipping implements ShippingMethod {
    private static final BigDecimal BASE_COST = new BigDecimal("12.00");
    private static final BigDecimal COST_PER_KG = new BigDecimal("2.00");
    private static final Integer DELIVERY_DAYS = 7;

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
        return "ECONOMICA";
    }
}
