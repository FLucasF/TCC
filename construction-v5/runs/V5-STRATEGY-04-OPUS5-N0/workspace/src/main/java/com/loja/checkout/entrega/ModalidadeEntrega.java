package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Identificavel;
import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * Uma forma de entrega. Cada parceria nova é uma implementação desta interface:
 * diz quanto cobra, em quantos dias entrega e quais pedidos aceita.
 */
public interface ModalidadeEntrega extends Identificavel {

    /** Frete para o pedido, arredondado para centavos. */
    BigDecimal frete(Pedido pedido);

    /** Prazo de entrega em dias. */
    int prazoEntregaDias();

    /** Se esta modalidade atende o pedido (peso, região, o que for). */
    default boolean atende(Pedido pedido) {
        return true;
    }
}
