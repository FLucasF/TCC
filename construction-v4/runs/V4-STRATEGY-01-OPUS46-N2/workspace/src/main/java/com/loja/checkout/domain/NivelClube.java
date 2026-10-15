package com.loja.checkout.domain;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE(BigDecimal.ZERO, false, false),
    PRATA(new BigDecimal("0.02"), false, false),
    OURO(new BigDecimal("0.05"), true, true);

    private final BigDecimal percentualCredito;
    private final boolean freteGratis;
    private final boolean elegivelBrinde;

    NivelClube(BigDecimal percentualCredito, boolean freteGratis, boolean elegivelBrinde) {
        this.percentualCredito = percentualCredito;
        this.freteGratis = freteGratis;
        this.elegivelBrinde = elegivelBrinde;
    }

    public BigDecimal percentualCredito() {
        return percentualCredito;
    }

    public boolean freteGratis() {
        return freteGratis;
    }

    public boolean elegivelBrinde() {
        return elegivelBrinde;
    }
}
