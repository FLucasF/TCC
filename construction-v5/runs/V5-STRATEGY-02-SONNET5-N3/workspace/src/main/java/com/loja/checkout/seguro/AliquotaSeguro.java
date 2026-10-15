package com.loja.checkout.seguro;

import java.math.BigDecimal;
import java.util.Map;

public final class AliquotaSeguro {

    private static final Map<String, BigDecimal> ALIQUOTAS = Map.of(
            "SUDESTE", new BigDecimal("0.01"),
            "SUL", new BigDecimal("0.01"),
            "CENTRO_OESTE", new BigDecimal("0.015"),
            "NORTE", new BigDecimal("0.025"),
            "NORDESTE", new BigDecimal("0.02")
    );

    private AliquotaSeguro() {
    }

    public static BigDecimal para(String regiao) {
        return ALIQUOTAS.get(regiao);
    }
}
