package com.loja.entrega;

public class FabricaEntrega {
    public static Entrega criar(String modalidade) {
        if (modalidade == null) {
            return null;
        }

        return switch (modalidade) {
            case "ECONOMICA" -> new EntregaEconomica();
            case "EXPRESSA" -> new EntregaExpressa();
            case "RETIRADA_LOJA" -> new EntregaRetiradaLoja();
            case "MOTOBOY" -> new EntregaMotoboy();
            default -> null;
        };
    }
}
