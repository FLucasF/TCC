package com.loja.checkout.model;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(BigDecimal.ZERO),
    PRATA(new BigDecimal("0.02")),
    OURO(new BigDecimal("0.05"));

    private final BigDecimal percentualCredito;

    NivelClube(BigDecimal percentualCredito) {
        this.percentualCredito = percentualCredito;
    }

    public BigDecimal getPercentualCredito() {
        return percentualCredito;
    }

    public boolean temFreteGratis() {
        return this == OURO;
    }

    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return this == OURO && subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
    }
}
