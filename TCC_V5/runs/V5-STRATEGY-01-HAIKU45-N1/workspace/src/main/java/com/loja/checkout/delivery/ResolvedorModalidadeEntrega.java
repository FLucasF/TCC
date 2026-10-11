package com.loja.checkout.delivery;

public class ResolvedorModalidadeEntrega {
    public static ModalidadeEntrega resolver(String nome) {
        if (nome == null) {
            return null;
        }

        return switch (nome.toUpperCase()) {
            case "ECONOMICA" -> new EntregaEconomica();
            case "EXPRESSA" -> new EntregaExpressa();
            case "RETIRADA_LOJA" -> new EntregaRetiraLoja();
            case "MOTOBOY" -> new EntregaMotoboy();
            default -> null;
        };
    }
}
