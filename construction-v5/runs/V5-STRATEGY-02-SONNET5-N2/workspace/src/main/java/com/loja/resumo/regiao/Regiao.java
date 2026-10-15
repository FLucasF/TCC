package com.loja.resumo.regiao;

import java.math.BigDecimal;
import java.util.Optional;

public enum Regiao {

    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));

    private final BigDecimal percentualSeguro;

    Regiao(BigDecimal percentualSeguro) {
        this.percentualSeguro = percentualSeguro;
    }

    public BigDecimal percentualSeguro() {
        return percentualSeguro;
    }

    public static Optional<Regiao> encontrar(String nome) {
        if (nome == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Regiao.valueOf(nome));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
