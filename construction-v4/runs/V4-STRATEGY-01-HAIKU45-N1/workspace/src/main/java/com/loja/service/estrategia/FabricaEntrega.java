package com.loja.service.estrategia;

import com.loja.domain.ModalidadeEntrega;

public class FabricaEntrega {
    public static CalculoEntrega criar(ModalidadeEntrega modalidade) {
        return switch (modalidade) {
            case ECONOMICA -> new EntregaEconomica();
            case EXPRESSA -> new EntregaExpressa();
            case RETIRADA_LOJA -> new EntregaRetiradaLoja();
            case MOTOBOY -> new EntregaMotoboy();
        };
    }
}
