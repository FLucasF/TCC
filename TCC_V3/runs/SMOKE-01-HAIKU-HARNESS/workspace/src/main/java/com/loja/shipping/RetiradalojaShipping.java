package com.loja.shipping;

import java.math.BigDecimal;

public class RetiradalojaShipping implements ShippingMethod {
    private static final BigDecimal COST = BigDecimal.ZERO;
    private static final Integer DELIVERY_DAYS = 1;

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
        return true;
    }

    @Override
    public String getCode() {
        return "RETIRADA_LOJA";
    }
}
