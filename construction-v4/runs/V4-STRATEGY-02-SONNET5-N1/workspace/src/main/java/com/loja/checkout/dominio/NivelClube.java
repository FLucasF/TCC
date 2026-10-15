package com.loja.checkout.dominio;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE(BigDecimal.ZERO, false, null),
    PRATA(new BigDecimal("0.02"), false, null),
    OURO(new BigDecimal("0.05"), true, new BigDecimal("500.00"));

    private final BigDecimal percentualCredito;
    private final boolean freteGratis;
    private final BigDecimal limiarBrinde;

    NivelClube(BigDecimal percentualCredito, boolean freteGratis, BigDecimal limiarBrinde) {
        this.percentualCredito = percentualCredito;
        this.freteGratis = freteGratis;
        this.limiarBrinde = limiarBrinde;
    }

    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(percentualCredito));
    }

    public boolean freteGratis() {
        return freteGratis;
    }

    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return limiarBrinde != null && subtotalProdutos.compareTo(limiarBrinde) > 0;
    }
}
