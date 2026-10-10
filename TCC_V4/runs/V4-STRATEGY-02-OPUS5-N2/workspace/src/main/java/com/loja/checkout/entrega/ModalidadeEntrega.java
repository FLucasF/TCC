package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Identificado;
import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * Uma forma de o cliente receber o pedido: quanto custa, em quantos dias chega
 * e quais pedidos ela atende.
 */
public interface ModalidadeEntrega extends Identificado {

    BigDecimal frete(Pedido pedido);

    int prazoDias();

    default boolean atende(Pedido pedido) {
        return true;
    }
}
