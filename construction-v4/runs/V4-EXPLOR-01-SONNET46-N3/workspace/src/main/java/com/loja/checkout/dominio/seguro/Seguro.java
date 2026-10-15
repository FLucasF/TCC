package com.loja.checkout.dominio.seguro;

import com.loja.checkout.infra.CheckoutException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class Seguro {

    private static final Map<String, BigDecimal> TAXAS = Map.of(
            "SUDESTE", new BigDecimal("0.01"),
            "SUL", new BigDecimal("0.01"),
            "CENTRO_OESTE", new BigDecimal("0.015"),
            "NORTE", new BigDecimal("0.025"),
            "NORDESTE", new BigDecimal("0.02")
    );

    public static void validarRegiao(String regiao) {
        if (regiao == null || !TAXAS.containsKey(regiao)) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }

    public static BigDecimal calcular(String regiao, BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(TAXAS.get(regiao)).setScale(2, RoundingMode.HALF_EVEN);
    }
}
