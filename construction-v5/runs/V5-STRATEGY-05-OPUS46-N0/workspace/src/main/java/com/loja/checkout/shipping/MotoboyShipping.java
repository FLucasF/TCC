package com.loja.checkout.shipping;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MotoboyShipping implements ShippingStrategy {

    private static final BigDecimal COST = new BigDecimal("18.00");
    private static final BigDecimal MAX_WEIGHT = new BigDecimal("5");

    @Override
    public String getCode() {
        return "MOTOBOY";
    }

    @Override
    public BigDecimal calculateCost(BigDecimal totalWeightKg) {
        return COST;
    }

    @Override
    public int getDeliveryDays() {
        return 0;
    }

    @Override
    public boolean isAvailable(BigDecimal totalWeightKg) {
        return totalWeightKg.compareTo(MAX_WEIGHT) <= 0;
    }
}
