package com.loja.clube;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(BigDecimal.ZERO, false),
    PRATA(BigDecimal.valueOf(0.02), false),
    OURO(BigDecimal.valueOf(0.05), true);

    private final BigDecimal percentualCredito;
    private final boolean naoPageFrete;

    NivelClube(BigDecimal percentualCredito, boolean naoPageFrete) {
        this.percentualCredito = percentualCredito;
        this.naoPageFrete = naoPageFrete;
    }

    public BigDecimal getPercentualCredito() {
        return percentualCredito;
    }

    public boolean isNaoPageFrete() {
        return naoPageFrete;
    }

    public static NivelClube porCodigo(String codigo) {
        try {
            return NivelClube.valueOf(codigo);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
