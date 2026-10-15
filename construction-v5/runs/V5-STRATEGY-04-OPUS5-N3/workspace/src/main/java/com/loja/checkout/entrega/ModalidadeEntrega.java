package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/** Uma opção de entrega: como cobra o frete, em quanto tempo entrega e o que atende. */
public interface ModalidadeEntrega {

    String codigo();

    BigDecimal frete(Pedido pedido);

    int prazoDias();

    /** Se a modalidade atende este pedido (ex.: motoboy só até 5 kg). */
    default boolean atende(Pedido pedido) {
        return true;
    }
}
