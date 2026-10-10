package com.loja.checkout.enums;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(BigDecimal.ZERO, false, false),
    PRATA(new BigDecimal("0.02"), false, false),
    OURO(new BigDecimal("0.05"), true, true);

    private final BigDecimal percentualCredito;
    private final Boolean freteGratis;
    private final Boolean ganhaBrinde;

    NivelClube(BigDecimal percentualCredito, Boolean freteGratis, Boolean ganhaBrinde) {
        this.percentualCredito = percentualCredito;
        this.freteGratis = freteGratis;
        this.ganhaBrinde = ganhaBrinde;
    }

    public BigDecimal getPercentualCredito() {
        return percentualCredito;
    }

    public Boolean getFreteGratis() {
        return freteGratis;
    }

    public Boolean getGanhaBrinde() {
        return ganhaBrinde;
    }
}
