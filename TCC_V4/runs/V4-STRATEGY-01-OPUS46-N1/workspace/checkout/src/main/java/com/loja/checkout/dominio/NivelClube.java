package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum NivelClube {

    BRONZE(BigDecimal.ZERO, false, null),
    PRATA(new BigDecimal("0.02"), false, null),
    OURO(new BigDecimal("0.05"), true, new BigDecimal("500"));

    private final BigDecimal percentualCredito;
    private final boolean freteGratis;
    private final BigDecimal limiarBrinde;

    NivelClube(BigDecimal percentualCredito, boolean freteGratis, BigDecimal limiarBrinde) {
        this.percentualCredito = percentualCredito;
        this.freteGratis = freteGratis;
        this.limiarBrinde = limiarBrinde;
    }

    public boolean isFreteGratis() {
        return freteGratis;
    }

    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(percentualCredito).setScale(2, RoundingMode.HALF_EVEN);
    }

    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return limiarBrinde != null && subtotalProdutos.compareTo(limiarBrinde) > 0;
    }
}
