package com.loja.checkout.estrategia;

import com.loja.checkout.domain.ModalidadeEntrega;

public class FabricaEntrega {
    public static EstrategiaEntrega criar(ModalidadeEntrega modalidade) {
        return switch (modalidade) {
            case ECONOMICA -> new EntregaEconomica();
            case EXPRESSA -> new EntregaExpressa();
            case RETIRADA_LOJA -> new EntregaRetiradaLoja();
            case MOTOBOY -> new EntregaMotoboy();
        };
    }
}
