package com.loja.checkout.regiao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private static final Map<String, Regiao> POR_CODIGO = Map.of(
            "SUDESTE", SUDESTE,
            "SUL", SUL,
            "CENTRO_OESTE", CENTRO_OESTE,
            "NORTE", NORTE,
            "NORDESTE", NORDESTE
    );

    private final BigDecimal taxaSeguro;

    Regiao(String taxa) {
        this.taxaSeguro = new BigDecimal(taxa);
    }

    public BigDecimal calcularSeguro(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(taxaSeguro).setScale(2, RoundingMode.HALF_EVEN);
    }

    public static Regiao fromCodigo(String codigo) {
        return codigo == null ? null : POR_CODIGO.get(codigo);
    }
}
