package com.loja.checkout.model;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(BigDecimal.ZERO, false, false),
    PRATA(new BigDecimal("0.02"), false, false),
    OURO(new BigDecimal("0.05"), true, true);

    private final BigDecimal taxaCredito;
    private final boolean semFrete;
    private final boolean temBrinde;

    NivelClube(BigDecimal taxaCredito, boolean semFrete, boolean temBrinde) {
        this.taxaCredito = taxaCredito;
        this.semFrete = semFrete;
        this.temBrinde = temBrinde;
    }

    public BigDecimal getTaxaCredito() {
        return taxaCredito;
    }

    public boolean isSemFrete() {
        return semFrete;
    }

    public boolean isTemBrinde() {
        return temBrinde;
    }
}
