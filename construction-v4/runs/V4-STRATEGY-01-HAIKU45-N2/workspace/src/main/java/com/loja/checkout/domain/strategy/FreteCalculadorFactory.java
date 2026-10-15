package com.loja.checkout.domain.strategy;

public class FreteCalculadorFactory {
    public static FreteCalculador criar(String modalidade) {
        if (modalidade == null) {
            return null;
        }

        return switch (modalidade) {
            case "ECONOMICA" -> new FreteEconomica();
            case "EXPRESSA" -> new FreteExpressa();
            case "RETIRADA_LOJA" -> new FreteRetiradaLoja();
            case "MOTOBOY" -> new FreteMotoboy();
            default -> null;
        };
    }
}
