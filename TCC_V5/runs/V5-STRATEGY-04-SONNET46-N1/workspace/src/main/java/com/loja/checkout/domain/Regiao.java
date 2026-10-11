package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.Map;

public enum Regiao {
    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));

    public final BigDecimal taxaSeguro;

    Regiao(BigDecimal taxaSeguro) {
        this.taxaSeguro = taxaSeguro;
    }

    private static final Map<String, Regiao> POR_NOME = Map.of(
        "SUDESTE", SUDESTE,
        "SUL", SUL,
        "CENTRO_OESTE", CENTRO_OESTE,
        "NORTE", NORTE,
        "NORDESTE", NORDESTE
    );

    public static Regiao buscar(String nome) {
        return POR_NOME.get(nome);
    }
}
