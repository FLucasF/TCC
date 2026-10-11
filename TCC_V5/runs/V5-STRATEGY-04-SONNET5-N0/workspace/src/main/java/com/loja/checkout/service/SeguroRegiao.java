package com.loja.checkout.service;

import java.math.BigDecimal;
import java.util.Map;

public final class SeguroRegiao {

    private static final Map<String, BigDecimal> PERCENTUAIS = Map.of(
            "SUDESTE", new BigDecimal("0.01"),
            "SUL", new BigDecimal("0.01"),
            "CENTRO_OESTE", new BigDecimal("0.015"),
            "NORTE", new BigDecimal("0.025"),
            "NORDESTE", new BigDecimal("0.02")
    );

    private SeguroRegiao() {
    }

    public static boolean existe(String regiao) {
        return PERCENTUAIS.containsKey(regiao);
    }

    public static BigDecimal percentual(String regiao) {
        return PERCENTUAIS.get(regiao);
    }
}
