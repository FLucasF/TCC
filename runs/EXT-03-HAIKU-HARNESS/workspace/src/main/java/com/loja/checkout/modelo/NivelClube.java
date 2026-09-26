package com.loja.checkout.modelo;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(BigDecimal.ZERO, false),
    PRATA(new BigDecimal("0.02"), false),
    OURO(new BigDecimal("0.05"), true);

    private final BigDecimal percentualCredito;
    private final boolean freteGratis;

    NivelClube(BigDecimal percentualCredito, boolean freteGratis) {
        this.percentualCredito = percentualCredito;
        this.freteGratis = freteGratis;
    }

    public BigDecimal getPercentualCredito() {
        return percentualCredito;
    }

    public boolean isFreteGratis() {
        return freteGratis;
    }
}
