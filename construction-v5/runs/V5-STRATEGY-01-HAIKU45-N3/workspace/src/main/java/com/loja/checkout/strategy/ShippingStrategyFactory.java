package com.loja.checkout.strategy;

import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.strategy.shipping.*;

public class ShippingStrategyFactory {
    public static ShippingStrategy create(ModalidadeEntrega modalidade) {
        return switch (modalidade) {
            case ECONOMICA -> new EconomicaShipping();
            case EXPRESSA -> new ExpressaShipping();
            case RETIRADA_LOJA -> new RetiradaLojaShipping();
            case MOTOBOY -> new MotoboyCourier();
        };
    }
}
