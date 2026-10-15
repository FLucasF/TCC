package com.loja.checkout.model;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(new BigDecimal("0"), false),
    PRATA(new BigDecimal("0.02"), false),
    OURO(new BigDecimal("0.05"), true);

    private final BigDecimal percentualCredito;
    private final boolean fretGratis;

    NivelClube(BigDecimal percentualCredito, boolean fretGratis) {
        this.percentualCredito = percentualCredito;
        this.fretGratis = fretGratis;
    }

    public BigDecimal getPercentualCredito() {
        return percentualCredito;
    }

    public boolean isFretGratis() {
        return fretGratis;
    }
}
