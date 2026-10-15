package com.loja.delivery;

import java.math.BigDecimal;

public class MotoboyCost implements DeliveryMode {
    @Override
    public String getCode() {
        return "MOTOBOY";
    }

    @Override
    public BigDecimal calculateShipping(double totalWeightKg) {
        return new BigDecimal("18.00");
    }

    @Override
    public int getDeliveryDays() {
        return 0;
    }

    @Override
    public boolean isAvailable(double totalWeightKg) {
        return totalWeightKg <= 5.0;
    }
}
