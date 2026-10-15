package com.loja.checkout.model;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE(BigDecimal.ZERO, false, false),
    PRATA(new BigDecimal("0.02"), false, false),
    OURO(new BigDecimal("0.05"), true, true);

    private final BigDecimal creditRate;
    private final boolean freeShipping;
    private final boolean giftEligible;

    NivelClube(BigDecimal creditRate, boolean freeShipping, boolean giftEligible) {
        this.creditRate = creditRate;
        this.freeShipping = freeShipping;
        this.giftEligible = giftEligible;
    }

    public BigDecimal getCreditRate() {
        return creditRate;
    }

    public boolean hasFreeShipping() {
        return freeShipping;
    }

    public boolean isGiftEligible() {
        return giftEligible;
    }

    public static NivelClube fromString(String value) {
        if (value == null) return null;
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
