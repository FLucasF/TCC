package com.loja.checkout;

import java.math.BigDecimal;
import java.util.Map;

public final class Seguro {

    private static final Map<String, BigDecimal> TAXAS = Map.of(
            "SUDESTE", new BigDecimal("0.01"),
            "SUL", new BigDecimal("0.01"),
            "CENTRO_OESTE", new BigDecimal("0.015"),
            "NORTE", new BigDecimal("0.025"),
            "NORDESTE", new BigDecimal("0.02")
    );

    private Seguro() {}

    public static boolean regiaoValida(String regiao) {
        return regiao != null && TAXAS.containsKey(regiao);
    }

    public static BigDecimal calcular(String regiao, BigDecimal subtotalProdutos) {
        return Arredondamento.centavos(subtotalProdutos.multiply(TAXAS.get(regiao)));
    }
}
