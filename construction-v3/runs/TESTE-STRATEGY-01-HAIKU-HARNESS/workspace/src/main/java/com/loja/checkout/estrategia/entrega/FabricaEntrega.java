package com.loja.checkout.estrategia.entrega;

import com.loja.checkout.modelo.ModalidadeEntrega;

public class FabricaEntrega {
    public static EstrategiaEntrega criar(String modalidadeStr) {
        if (modalidadeStr == null || modalidadeStr.isBlank()) {
            throw new IllegalArgumentException("MODALIDADE_INVALIDA");
        }
        try {
            ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(modalidadeStr);
            return switch (modalidade) {
                case ECONOMICA -> new EntregaEconomica();
                case EXPRESSA -> new EntregaExpressa();
                case RETIRADA_LOJA -> new EntregaRetiradaLoja();
                case MOTOBOY -> new EntregaMotoboy();
            };
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("MODALIDADE_INVALIDA");
        }
    }
}
