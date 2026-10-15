package com.loja.checkout.regiao;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Regiao {

    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));

    private static final Map<String, Regiao> POR_NOME = Stream.of(values())
            .collect(Collectors.toMap(Enum::name, Function.identity()));

    private final BigDecimal taxaSeguro;

    Regiao(BigDecimal taxaSeguro) {
        this.taxaSeguro = taxaSeguro;
    }

    public BigDecimal taxaSeguro() {
        return taxaSeguro;
    }

    public static Regiao porNome(String nome) {
        return nome == null ? null : POR_NOME.get(nome);
    }
}
