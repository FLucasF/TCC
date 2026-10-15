package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

public enum Regiao {

    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));

    private static final Map<String, Regiao> POR_CODIGO = Map.of(
            "SUDESTE", SUDESTE,
            "SUL", SUL,
            "CENTRO_OESTE", CENTRO_OESTE,
            "NORTE", NORTE,
            "NORDESTE", NORDESTE
    );

    private final BigDecimal taxaSeguro;

    Regiao(BigDecimal taxaSeguro) {
        this.taxaSeguro = taxaSeguro;
    }

    public BigDecimal getTaxaSeguro() {
        return taxaSeguro;
    }

    public static Optional<Regiao> buscar(String codigo) {
        return Optional.ofNullable(POR_CODIGO.get(codigo));
    }
}
