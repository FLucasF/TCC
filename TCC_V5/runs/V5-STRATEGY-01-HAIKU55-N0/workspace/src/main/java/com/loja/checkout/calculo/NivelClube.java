package com.loja.checkout.calculo;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE("0", false),
    PRATA("0.02", false),
    OURO("0.05", true);

    private final BigDecimal taxaCredito;
    private final boolean freteGratis;

    NivelClube(String taxaCredito, boolean freteGratis) {
        this.taxaCredito = new BigDecimal(taxaCredito);
        this.freteGratis = freteGratis;
    }

    BigDecimal taxaCredito() {
        return taxaCredito;
    }

    boolean freteGratis() {
        return freteGratis;
    }
}
