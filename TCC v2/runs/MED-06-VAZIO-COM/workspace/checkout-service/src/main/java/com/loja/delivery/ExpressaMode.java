package com.loja.delivery;

import java.math.BigDecimal;
import com.loja.util.MoneyRounder;

public class ExpressaMode implements DeliveryMode {
    @Override
    public String getCode() {
        return "EXPRESSA";
    }

    @Override
    public BigDecimal calculateShipping(double totalWeightKg) {
        BigDecimal base = new BigDecimal("25.00");
        BigDecimal weightCost = new BigDecimal(String.valueOf(totalWeightKg)).multiply(new BigDecimal("4.50"));
        return MoneyRounder.round(base.add(weightCost));
    }

    @Override
    public int getDeliveryDays() {
        return 2;
    }

    @Override
    public boolean isAvailable(double totalWeightKg) {
        return true;
    }
}
