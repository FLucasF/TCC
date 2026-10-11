package com.loja.checkout.domain;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(BigDecimal.ZERO, false, false),
    PRATA(new BigDecimal("0.02"), false, false),
    OURO(new BigDecimal("0.05"), true, true);

    private final BigDecimal taxaCredito;
    private final boolean freteGratis;
    private final boolean brindePossivel;

    NivelClube(BigDecimal taxaCredito, boolean freteGratis, boolean brindePossivel) {
        this.taxaCredito = taxaCredito;
        this.freteGratis = freteGratis;
        this.brindePossivel = brindePossivel;
    }

    public BigDecimal getTaxaCredito() {
        return taxaCredito;
    }

    public boolean isFreteGratis() {
        return freteGratis;
    }

    public boolean isBrindePossivel() {
        return brindePossivel;
    }
}
