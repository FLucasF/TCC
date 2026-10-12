package com.loja.checkout.shipping;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RetiradaLojaShipping implements ShippingStrategy {

    @Override
    public String getCode() {
        return "RETIRADA_LOJA";
    }

    @Override
    public BigDecimal calculateCost(BigDecimal totalWeightKg) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int getDeliveryDays() {
        return 1;
    }

    @Override
    public boolean isAvailable(BigDecimal totalWeightKg) {
        return true;
    }
}
