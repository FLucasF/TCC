package com.loja.domain.clube;

import java.math.BigDecimal;

public class Beneficios {
    private final BigDecimal credito;
    private final boolean freteGratis;
    private final boolean brinde;

    public Beneficios(BigDecimal credito, boolean freteGratis, boolean brinde) {
        this.credito = credito;
        this.freteGratis = freteGratis;
        this.brinde = brinde;
    }

    public BigDecimal getCredito() {
        return credito;
    }

    public boolean isFreteGratis() {
        return freteGratis;
    }

    public boolean isBrinde() {
        return brinde;
    }
}
