package com.loja.checkout.calculo;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE(BigDecimal.ZERO),
    PRATA(new BigDecimal("0.02")),
    OURO(new BigDecimal("0.05"));

    private final BigDecimal percentualCredito;

    NivelClube(BigDecimal percentualCredito) {
        this.percentualCredito = percentualCredito;
    }

    public BigDecimal percentualCredito() {
        return percentualCredito;
    }
}
