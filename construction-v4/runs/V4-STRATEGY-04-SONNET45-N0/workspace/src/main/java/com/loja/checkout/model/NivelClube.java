package com.loja.checkout.model;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(BigDecimal.ZERO, false, false),
    PRATA(new BigDecimal("0.02"), false, false),
    OURO(new BigDecimal("0.05"), true, true);

    private final BigDecimal percentualCredito;
    private final boolean freteGratis;
    private final boolean recebeBrinde;

    NivelClube(BigDecimal percentualCredito, boolean freteGratis, boolean recebeBrinde) {
        this.percentualCredito = percentualCredito;
        this.freteGratis = freteGratis;
        this.recebeBrinde = recebeBrinde;
    }

    public BigDecimal getPercentualCredito() {
        return percentualCredito;
    }

    public boolean isFreteGratis() {
        return freteGratis;
    }

    public boolean isRecebeBrinde() {
        return recebeBrinde;
    }
}
