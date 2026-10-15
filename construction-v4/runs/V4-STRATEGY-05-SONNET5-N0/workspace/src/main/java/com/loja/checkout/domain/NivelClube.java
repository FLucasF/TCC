package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Nivel do cliente no clube da loja e as vantagens associadas.
 */
public enum NivelClube {

    BRONZE(BigDecimal.ZERO, false, null),
    PRATA(new BigDecimal("0.02"), false, null),
    OURO(new BigDecimal("0.05"), true, new BigDecimal("500.00"));

    private final BigDecimal percentualCredito;
    private final boolean freteGratis;
    private final BigDecimal limiteBrindeAcimaDe;

    NivelClube(BigDecimal percentualCredito, boolean freteGratis, BigDecimal limiteBrindeAcimaDe) {
        this.percentualCredito = percentualCredito;
        this.freteGratis = freteGratis;
        this.limiteBrindeAcimaDe = limiteBrindeAcimaDe;
    }

    public BigDecimal percentualCredito() {
        return percentualCredito;
    }

    public boolean freteGratis() {
        return freteGratis;
    }

    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return limiteBrindeAcimaDe != null && subtotalProdutos.compareTo(limiteBrindeAcimaDe) > 0;
    }
}
