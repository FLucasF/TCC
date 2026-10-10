package com.loja.checkout.model;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE("0", false),
    PRATA("0.02", false),
    OURO("0.05", true);

    private final BigDecimal percentualCredito;
    private final boolean fretGratis;

    NivelClube(String percentualCredito, boolean freteGratis) {
        this.percentualCredito = new BigDecimal(percentualCredito);
        this.fretGratis = freteGratis;
    }

    public BigDecimal getPercentualCredito() {
        return percentualCredito;
    }

    public boolean temFreteGratis() {
        return fretGratis;
    }
}
