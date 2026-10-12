package com.loja.checkout.shipping;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class EconomicaShipping implements ShippingStrategy {

    private static final BigDecimal BASE = new BigDecimal("12.00");
    private static final BigDecimal PER_KG = new BigDecimal("2.00");

    @Override
    public String getCode() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal calculateCost(BigDecimal totalWeightKg) {
        return BASE.add(PER_KG.multiply(totalWeightKg)).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int getDeliveryDays() {
        return 7;
    }

    @Override
    public boolean isAvailable(BigDecimal totalWeightKg) {
        return true;
    }
}
