package com.loja.checkout.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum NivelClube {

    BRONZE(BigDecimal.ZERO, false, false),
    PRATA(new BigDecimal("0.02"), false, false),
    OURO(new BigDecimal("0.05"), true, true);

    private final BigDecimal taxaCredito;
    private final boolean freteGratis;
    private final boolean elegiBrinde;

    NivelClube(BigDecimal taxaCredito, boolean freteGratis, boolean elegiBrinde) {
        this.taxaCredito = taxaCredito;
        this.freteGratis = freteGratis;
        this.elegiBrinde = elegiBrinde;
    }

    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(taxaCredito).setScale(2, RoundingMode.HALF_EVEN);
    }

    public boolean isFreteGratis() {
        return freteGratis;
    }

    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return elegiBrinde && subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
    }
}
