package com.loja.checkout.enums;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(new BigDecimal("0.00"), false, false),
    PRATA(new BigDecimal("0.02"), false, false),
    OURO(new BigDecimal("0.05"), true, true);

    private final BigDecimal percentualCredito;
    private final boolean freteGratis;
    private final boolean habilitaBrinde;

    NivelClube(BigDecimal percentualCredito, boolean freteGratis, boolean habilitaBrinde) {
        this.percentualCredito = percentualCredito;
        this.freteGratis = freteGratis;
        this.habilitaBrinde = habilitaBrinde;
    }

    public BigDecimal getPercentualCredito() {
        return percentualCredito;
    }

    public boolean temFreteGratis() {
        return freteGratis;
    }

    public boolean temBrinde() {
        return habilitaBrinde;
    }
}
