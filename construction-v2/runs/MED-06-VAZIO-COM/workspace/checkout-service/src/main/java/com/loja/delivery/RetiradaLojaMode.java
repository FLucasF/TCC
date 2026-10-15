package com.loja.delivery;

import java.math.BigDecimal;

public class RetiradaLojaMode implements DeliveryMode {
    @Override
    public String getCode() {
        return "RETIRADA_LOJA";
    }

    @Override
    public BigDecimal calculateShipping(double totalWeightKg) {
        return BigDecimal.ZERO;
    }

    @Override
    public int getDeliveryDays() {
        return 1;
    }

    @Override
    public boolean isAvailable(double totalWeightKg) {
        return true;
    }
}
