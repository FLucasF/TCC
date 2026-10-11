package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.Arrays;

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

    public BigDecimal getTaxa() {
        return taxa;
    }

    public static Regiao buscar(String codigo) {
        if (codigo == null) return null;
        return Arrays.stream(values())
                .filter(r -> r.name().equals(codigo))
                .findFirst()
                .orElse(null);
    }
}
