package com.loja.checkout.domain.regiao;

import java.math.BigDecimal;
import java.util.Map;

public enum Regiao {

    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));

    private final BigDecimal taxaSeguro;

    private static final Map<String, Regiao> POR_CODIGO = Map.of(
            "SUDESTE", SUDESTE,
            "SUL", SUL,
            "CENTRO_OESTE", CENTRO_OESTE,
            "NORTE", NORTE,
            "NORDESTE", NORDESTE
    );

    Regiao(BigDecimal taxaSeguro) {
        this.taxaSeguro = taxaSeguro;
    }

    public BigDecimal getTaxaSeguro() {
        return taxaSeguro;
    }

    public static Regiao porCodigo(String codigo) {
        return POR_CODIGO.get(codigo);
    }
}
