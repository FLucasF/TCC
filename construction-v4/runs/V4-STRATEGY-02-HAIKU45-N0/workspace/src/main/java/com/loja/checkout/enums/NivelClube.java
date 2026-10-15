package com.loja.checkout.enums;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(BigDecimal.ZERO, false),
    PRATA(new BigDecimal("0.02"), false),
    OURO(new BigDecimal("0.05"), true);

    private final BigDecimal creditoPercentual;
    private final boolean freteGratis;

    NivelClube(BigDecimal creditoPercentual, boolean freteGratis) {
        this.creditoPercentual = creditoPercentual;
        this.freteGratis = freteGratis;
    }

    public BigDecimal getCreditoPercentual() {
        return creditoPercentual;
    }

    public boolean isFreteGratis() {
        return freteGratis;
    }
}
