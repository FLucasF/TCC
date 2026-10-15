package com.loja.checkout.pedido;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(new BigDecimal("0"), false, null),
    PRATA(new BigDecimal("0.02"), false, null),
    OURO(new BigDecimal("0.05"), true, new BigDecimal("500.00"));

    private final BigDecimal percentualCredito;
    private final boolean freteGratis;
    private final BigDecimal limiteBrinde;

    NivelClube(BigDecimal percentualCredito, boolean freteGratis, BigDecimal limiteBrinde) {
        this.percentualCredito = percentualCredito;
        this.freteGratis = freteGratis;
        this.limiteBrinde = limiteBrinde;
    }

    public BigDecimal getPercentualCredito() {
        return percentualCredito;
    }

    public boolean isFreteGratis() {
        return freteGratis;
    }

    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return limiteBrinde != null && subtotalProdutos.compareTo(limiteBrinde) > 0;
    }
}
