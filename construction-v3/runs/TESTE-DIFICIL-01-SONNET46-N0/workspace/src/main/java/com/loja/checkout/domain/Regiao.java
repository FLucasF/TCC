package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

public enum Regiao {
    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal taxaSeguro;

    Regiao(String taxa) {
        this.taxaSeguro = new BigDecimal(taxa);
    }

    public BigDecimal getTaxaSeguro() {
        return taxaSeguro;
    }

    public static Optional<Regiao> buscar(String codigo) {
        if (codigo == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(r -> r.name().equals(codigo))
                .findFirst();
    }
}
