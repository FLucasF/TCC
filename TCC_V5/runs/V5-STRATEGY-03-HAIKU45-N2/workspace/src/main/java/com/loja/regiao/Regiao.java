package com.loja.regiao;

import java.math.BigDecimal;

public enum Regiao {
    SUDESTE(BigDecimal.valueOf(0.01)),
    SUL(BigDecimal.valueOf(0.01)),
    CENTRO_OESTE(BigDecimal.valueOf(0.015)),
    NORTE(BigDecimal.valueOf(0.025)),
    NORDESTE(BigDecimal.valueOf(0.02));

    private final BigDecimal percentualSeguro;

    Regiao(BigDecimal percentualSeguro) {
        this.percentualSeguro = percentualSeguro;
    }

    public BigDecimal getPercentualSeguro() {
        return percentualSeguro;
    }

    public static Regiao porCodigo(String codigo) {
        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
