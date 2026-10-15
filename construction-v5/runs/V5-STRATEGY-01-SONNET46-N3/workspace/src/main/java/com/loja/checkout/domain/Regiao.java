package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.Optional;

public enum Regiao {
    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));

    private final BigDecimal taxa;

    Regiao(BigDecimal taxa) {
        this.taxa = taxa;
    }

    public BigDecimal taxa() {
        return taxa;
    }

    public static Optional<Regiao> buscar(String codigo) {
        if (codigo == null) return Optional.empty();
        try {
            return Optional.of(valueOf(codigo));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
