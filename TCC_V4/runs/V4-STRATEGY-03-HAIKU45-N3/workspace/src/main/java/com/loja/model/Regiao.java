package com.loja.model;

import java.math.BigDecimal;

public enum Regiao {
    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));

    private final BigDecimal seguroPercent;

    Regiao(BigDecimal seguroPercent) {
        this.seguroPercent = seguroPercent;
    }

    public BigDecimal getSeguroPercent() {
        return seguroPercent;
    }
}
