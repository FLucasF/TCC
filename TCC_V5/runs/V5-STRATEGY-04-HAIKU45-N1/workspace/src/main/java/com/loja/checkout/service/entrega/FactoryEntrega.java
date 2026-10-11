package com.loja.checkout.service.entrega;

import com.loja.checkout.model.ModalidadeEntrega;

public class FactoryEntrega {
    public static EstrategiaEntrega criar(ModalidadeEntrega modalidade) {
        return switch (modalidade) {
            case ECONOMICA -> new EstrategiaEntrega.Economica();
            case EXPRESSA -> new EstrategiaEntrega.Expressa();
            case RETIRADA_LOJA -> new EstrategiaEntrega.RetiraLoja();
            case MOTOBOY -> new EstrategiaEntrega.Motoboy();
        };
    }
}
