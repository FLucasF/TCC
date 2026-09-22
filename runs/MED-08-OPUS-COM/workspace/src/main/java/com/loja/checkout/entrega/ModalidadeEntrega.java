package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/** Uma opcao de entrega: quanto cobra, em quantos dias entrega e que pedidos aceita. */
public interface ModalidadeEntrega {

    String codigo();

    int prazoEntregaDias();

    BigDecimal frete(Pedido pedido);

    default boolean atende(Pedido pedido) {
        return true;
    }
}
