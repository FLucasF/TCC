package com.loja.checkout.domain;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(BigDecimal.ZERO, false, false),
    PRATA(new BigDecimal("0.02"), false, false),
    OURO(new BigDecimal("0.05"), true, true);

    private final BigDecimal percentualCredito;
    private final boolean freteGratis;
    private final boolean elegiveParaBrinde;

    NivelClube(BigDecimal percentualCredito, boolean freteGratis, boolean elegiveParaBrinde) {
        this.percentualCredito = percentualCredito;
        this.freteGratis = freteGratis;
        this.elegiveParaBrinde = elegiveParaBrinde;
    }

    public BigDecimal getPercentualCredito() {
        return percentualCredito;
    }

    public boolean isFreteGratis() {
        return freteGratis;
    }

    public boolean isElegiveParaBrinde() {
        return elegiveParaBrinde;
    }
}
