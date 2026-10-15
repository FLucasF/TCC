package com.loja.checkout.model;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(BigDecimal.ZERO, false, null),
    PRATA(new BigDecimal("0.02"), false, null),
    OURO(new BigDecimal("0.05"), true, new BigDecimal("500.00"));

    private final BigDecimal percentualCredito;
    private final boolean freteGratis;
    private final BigDecimal valorMinimoBrinde;

    NivelClube(BigDecimal percentualCredito, boolean freteGratis, BigDecimal valorMinimoBrinde) {
        this.percentualCredito = percentualCredito;
        this.freteGratis = freteGratis;
        this.valorMinimoBrinde = valorMinimoBrinde;
    }

    public BigDecimal getPercentualCredito() {
        return percentualCredito;
    }

    public boolean isFreteGratis() {
        return freteGratis;
    }

    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return valorMinimoBrinde != null && subtotalProdutos.compareTo(valorMinimoBrinde) > 0;
    }
}
