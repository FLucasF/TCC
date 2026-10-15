package com.loja.checkout.strategy;

public class EntregaFactory {

    public static Entrega criar(String modalidade) {
        return switch (modalidade) {
            case "ECONOMICA" -> new EntregaEconomica();
            case "EXPRESSA" -> new EntregaExpressa();
            case "RETIRADA_LOJA" -> new EntregaRetiradaLoja();
            case "MOTOBOY" -> new EntregaMotoboy();
            default -> null;
        };
    }

}
