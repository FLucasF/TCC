package com.loja.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum NivelClube {

    BRONZE(BigDecimal.ZERO, false, false),
    PRATA(new BigDecimal("0.02"), false, false),
    OURO(new BigDecimal("0.05"), true, true);

    private final BigDecimal creditoPercentual;
    private final boolean freteGratis;
    private final boolean elegivelBrinde;

    NivelClube(BigDecimal creditoPercentual, boolean freteGratis, boolean elegivelBrinde) {
        this.creditoPercentual = creditoPercentual;
        this.freteGratis = freteGratis;
        this.elegivelBrinde = elegivelBrinde;
    }

    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(creditoPercentual).setScale(2, RoundingMode.HALF_EVEN);
    }

    public boolean isFreteGratis() {
        return freteGratis;
    }

    public boolean ganharBrinde(BigDecimal subtotalProdutos) {
        return elegivelBrinde && subtotalProdutos.compareTo(new BigDecimal("500")) > 0;
    }
}
