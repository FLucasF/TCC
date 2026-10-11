package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum ShippingOption {
    ECONOMICA {
        @Override
        public BigDecimal cost(BigDecimal totalWeightKg) {
            return new BigDecimal("12.00")
                    .add(new BigDecimal("2.00").multiply(totalWeightKg))
                    .setScale(2, RoundingMode.HALF_EVEN);
        }
        @Override public int deliveryDays() { return 7; }
        @Override public boolean isAvailable(BigDecimal totalWeightKg) { return true; }
    },
    EXPRESSA {
        @Override
        public BigDecimal cost(BigDecimal totalWeightKg) {
            return new BigDecimal("25.00")
                    .add(new BigDecimal("4.50").multiply(totalWeightKg))
                    .setScale(2, RoundingMode.HALF_EVEN);
        }
        @Override public int deliveryDays() { return 2; }
        @Override public boolean isAvailable(BigDecimal totalWeightKg) { return true; }
    },
    RETIRADA_LOJA {
        @Override public BigDecimal cost(BigDecimal totalWeightKg) { return new BigDecimal("0.00"); }
        @Override public int deliveryDays() { return 1; }
        @Override public boolean isAvailable(BigDecimal totalWeightKg) { return true; }
    },
    MOTOBOY {
        @Override public BigDecimal cost(BigDecimal totalWeightKg) { return new BigDecimal("18.00"); }
        @Override public int deliveryDays() { return 0; }
        @Override
        public boolean isAvailable(BigDecimal totalWeightKg) {
            return totalWeightKg.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    public abstract BigDecimal cost(BigDecimal totalWeightKg);
    public abstract int deliveryDays();
    public abstract boolean isAvailable(BigDecimal totalWeightKg);
}
