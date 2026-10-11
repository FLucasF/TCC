package com.loja.checkout.domain;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(BigDecimal.ZERO, false, false),
    PRATA(BigDecimal.valueOf(0.02), false, false),
    OURO(BigDecimal.valueOf(0.05), true, true);

    private final BigDecimal percentualCredito;
    private final boolean fretegratis;
    private final boolean brindePeloValor;

    NivelClube(BigDecimal percentualCredito, boolean fretegratis, boolean brindePeloValor) {
        this.percentualCredito = percentualCredito;
        this.fretegratis = fretegratis;
        this.brindePeloValor = brindePeloValor;
    }

    public BigDecimal getPercentualCredito() {
        return percentualCredito;
    }

    public boolean isDeFrete() {
        return fretegratis;
    }

    public boolean temBrindePeloValor() {
        return brindePeloValor;
    }

    public static NivelClube fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return NivelClube.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
