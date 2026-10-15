package com.loja.shipping;

import java.math.BigDecimal;

public class MotoboyShipping implements ShippingMethod {
    private static final BigDecimal COST = new BigDecimal("18.00");
    private static final Integer DELIVERY_DAYS = 0;
    private static final BigDecimal MAX_WEIGHT_KG = new BigDecimal("5.00");

    @Override
    public BigDecimal calculateCost(BigDecimal totalWeightKg) {
        return COST;
    }

    @Override
    public Integer getDeliveryDays() {
        return DELIVERY_DAYS;
    }

    @Override
    public boolean isAvailable(BigDecimal totalWeightKg) {
        return totalWeightKg.compareTo(MAX_WEIGHT_KG) <= 0;
    }

    @Override
    public String getCode() {
        return "MOTOBOY";
    }
}
