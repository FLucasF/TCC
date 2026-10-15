package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Vantagens do clube da loja por nivel. Cada nivel novo e um novo valor de enum
 * com seu proprio conjunto de vantagens, sem alterar os demais.
 */
public enum NivelClube {

    BRONZE(BigDecimal.ZERO, false, null),
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

    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(percentualCredito));
    }

    public boolean isFreteGratis() {
        return freteGratis;
    }

    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return limiteBrinde != null && subtotalProdutos.compareTo(limiteBrinde) > 0;
    }
}
