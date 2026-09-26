package com.loja.checkout.modelo;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(new BigDecimal("0"), false, false),
    PRATA(new BigDecimal("0.02"), false, false),
    OURO(new BigDecimal("0.05"), true, true);

    private final BigDecimal percentualCredito;
    private final boolean fretGratis;
    private final boolean temBrinde;

    NivelClube(BigDecimal percentualCredito, boolean fretGratis, boolean temBrinde) {
        this.percentualCredito = percentualCredito;
        this.fretGratis = fretGratis;
        this.temBrinde = temBrinde;
    }

    public BigDecimal getPercentualCredito() {
        return percentualCredito;
    }

    public boolean isFretGratis() {
        return fretGratis;
    }

    public boolean isTemBrinde() {
        return temBrinde;
    }
}
