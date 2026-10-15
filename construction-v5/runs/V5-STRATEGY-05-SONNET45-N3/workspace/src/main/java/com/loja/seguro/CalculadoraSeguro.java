package com.loja.seguro;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CalculadoraSeguro {

    public static BigDecimal calcular(BigDecimal subtotalProdutos, String regiao) {
        BigDecimal porcentagem = switch (regiao) {
            case "SUDESTE", "SUL" -> new BigDecimal("0.01");
            case "CENTRO_OESTE" -> new BigDecimal("0.015");
            case "NORTE" -> new BigDecimal("0.025");
            case "NORDESTE" -> new BigDecimal("0.02");
            default -> null;
        };

        if (porcentagem == null) {
            return null;
        }

        return subtotalProdutos
            .multiply(porcentagem)
            .setScale(2, RoundingMode.HALF_EVEN);
    }
}
