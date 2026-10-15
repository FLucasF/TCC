package com.loja.checkout.model;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(BigDecimal.ZERO),
    PRATA(new BigDecimal("0.02")),
    OURO(new BigDecimal("0.05"));

    public final BigDecimal taxaCredito;

    NivelClube(BigDecimal taxaCredito) {
        this.taxaCredito = taxaCredito;
    }
}
