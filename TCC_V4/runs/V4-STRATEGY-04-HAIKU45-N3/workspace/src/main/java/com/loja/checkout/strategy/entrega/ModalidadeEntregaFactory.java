package com.loja.checkout.strategy.entrega;

import com.loja.checkout.model.ModalidadeEntrega;
import com.loja.checkout.exception.CheckoutException;

public class ModalidadeEntregaFactory {
    public static ModalidadeEntregaStrategy criar(ModalidadeEntrega modalidade) {
        if (modalidade == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }

        return switch (modalidade) {
            case ECONOMICA -> new EconomicaStrategy();
            case EXPRESSA -> new ExpressaStrategy();
            case RETIRADA_LOJA -> new RetiradaLojaStrategy();
            case MOTOBOY -> new MotoboyCStrategy();
        };
    }
}
