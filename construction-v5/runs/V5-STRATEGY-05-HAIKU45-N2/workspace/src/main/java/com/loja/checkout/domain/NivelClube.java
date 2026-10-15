package com.loja.checkout.domain;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(BigDecimal.ZERO, false),
    PRATA(new BigDecimal("0.02"), false),
    OURO(new BigDecimal("0.05"), true);

    private final BigDecimal creditoPorcentagem;
    private final boolean fretGratis;

    NivelClube(BigDecimal creditoPorcentagem, boolean fretGratis) {
        this.creditoPorcentagem = creditoPorcentagem;
        this.fretGratis = fretGratis;
    }

    public BigDecimal getCreditoPorcentagem() {
        return creditoPorcentagem;
    }

    public boolean isFretGratis() {
        return fretGratis;
    }
}
