package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum ClubLevel {
    BRONZE {
        @Override public BigDecimal cashbackRate() { return BigDecimal.ZERO; }
        @Override public boolean freeShipping() { return false; }
        @Override public boolean hasGiftAbove(BigDecimal subtotal) { return false; }
    },
    PRATA {
        @Override public BigDecimal cashbackRate() { return new BigDecimal("0.02"); }
        @Override public boolean freeShipping() { return false; }
        @Override public boolean hasGiftAbove(BigDecimal subtotal) { return false; }
    },
    OURO {
        @Override public BigDecimal cashbackRate() { return new BigDecimal("0.05"); }
        @Override public boolean freeShipping() { return true; }
        @Override
        public boolean hasGiftAbove(BigDecimal subtotal) {
            return subtotal.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    public abstract BigDecimal cashbackRate();
    public abstract boolean freeShipping();
    public abstract boolean hasGiftAbove(BigDecimal subtotal);

    public BigDecimal cashbackOn(BigDecimal subtotal) {
        return subtotal.multiply(cashbackRate()).setScale(2, RoundingMode.HALF_EVEN);
    }
}
