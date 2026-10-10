package com.loja.model;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(BigDecimal.ZERO),
    PRATA(new BigDecimal("0.02")),
    OURO(new BigDecimal("0.05"));

    private final BigDecimal creditoPercent;

    NivelClube(BigDecimal creditoPercent) {
        this.creditoPercent = creditoPercent;
    }

    public BigDecimal getCreditoPercent() {
        return creditoPercent;
    }

    public boolean isOuro() {
        return this == OURO;
    }

    public boolean isMemberWithCredit() {
        return this != BRONZE;
    }
}
