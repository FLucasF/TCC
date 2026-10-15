package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** O que cada opção de entrega precisa saber responder. */
public interface RegraEntrega {

    BigDecimal frete(Pedido pedido);

    int prazoDias();

    default boolean atende(Pedido pedido) {
        return true;
    }
}
