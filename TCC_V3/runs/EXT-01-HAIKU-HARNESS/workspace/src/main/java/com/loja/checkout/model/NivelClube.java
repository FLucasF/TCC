package com.loja.checkout.model;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(BigDecimal.ZERO, false),
    PRATA(new BigDecimal("0.02"), false),
    OURO(new BigDecimal("0.05"), true);

    private final BigDecimal taxaCredito;
    private final boolean freteGratis;

    NivelClube(BigDecimal taxaCredito, boolean freteGratis) {
        this.taxaCredito = taxaCredito;
        this.freteGratis = freteGratis;
    }

    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Arredonda.round(subtotalProdutos.multiply(taxaCredito));
    }

    public boolean temFreteGratis() {
        return freteGratis;
    }

    public boolean temBrinde(BigDecimal subtotalProdutos) {
        if (this != OURO) return false;
        return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
    }
}
