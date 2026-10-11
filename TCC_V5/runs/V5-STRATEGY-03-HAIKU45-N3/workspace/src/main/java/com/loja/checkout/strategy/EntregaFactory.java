package com.loja.checkout.strategy;

import com.loja.checkout.enums.ModalidadeEntrega;

public class EntregaFactory {

    public static EntregaStrategy criar(ModalidadeEntrega modalidade) {
        return switch (modalidade) {
            case ECONOMICA -> new EntregaEconomica();
            case EXPRESSA -> new EntregaExpressa();
            case RETIRADA_LOJA -> new EntregaRetiradaLoja();
            case MOTOBOY -> new EntregaMotoboy();
        };
    }
}
