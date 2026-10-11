package com.loja.checkout.entrega;

import com.loja.checkout.model.ModalidadeEntrega;

public class FabricaEntrega {
    public static CalculadoraEntrega criar(ModalidadeEntrega modalidade) {
        if (modalidade == null) {
            return null;
        }

        return switch (modalidade) {
            case ECONOMICA -> new EntregaEconomica();
            case EXPRESSA -> new EntregaExpressa();
            case RETIRADA_LOJA -> new EntregaRetiradaLoja();
            case MOTOBOY -> new EntregaMotoboy();
        };
    }
}
