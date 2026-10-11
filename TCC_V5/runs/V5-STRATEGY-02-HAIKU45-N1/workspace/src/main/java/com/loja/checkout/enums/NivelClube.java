package com.loja.checkout.enums;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(new BigDecimal("0.00"), false),
    PRATA(new BigDecimal("0.02"), false),
    OURO(new BigDecimal("0.05"), true);

    private final BigDecimal taxaCashback;
    private final boolean fretGratis;

    NivelClube(BigDecimal taxaCashback, boolean fretGratis) {
        this.taxaCashback = taxaCashback;
        this.fretGratis = fretGratis;
    }

    public BigDecimal getTaxaCashback() {
        return taxaCashback;
    }

    public boolean isFretGratis() {
        return fretGratis;
    }
}
