package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Codificado;
import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/** Uma opcao de entrega: seu jeito de cobrar, seu prazo e suas limitacoes. */
public interface ModalidadeEntrega extends Codificado {

    BigDecimal frete(Pedido pedido);

    int prazoEntregaDias();

    /** Se a opcao atende este pedido. */
    default boolean atende(Pedido pedido) {
        return true;
    }
}
