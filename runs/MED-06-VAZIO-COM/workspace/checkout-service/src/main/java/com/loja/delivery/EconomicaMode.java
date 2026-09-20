package com.loja.delivery;

import java.math.BigDecimal;
import com.loja.util.MoneyRounder;

public class EconomicaMode implements DeliveryMode {
    @Override
    public String getCode() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal calculateShipping(double totalWeightKg) {
        BigDecimal base = new BigDecimal("12.00");
        BigDecimal weightCost = new BigDecimal(String.valueOf(totalWeightKg)).multiply(new BigDecimal("2.00"));
        return MoneyRounder.round(base.add(weightCost));
    }

    @Override
    public int getDeliveryDays() {
        return 7;
    }

    @Override
    public boolean isAvailable(double totalWeightKg) {
        return true;
    }
}
