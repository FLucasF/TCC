package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Catalogavel;
import com.loja.checkout.dominio.Pedido;

/**
 * Uma forma de o cliente receber o pedido. Cada parceria nova de entrega e uma
 * implementacao desta interface, com seu jeito de cobrar, seu prazo e seus
 * limites.
 */
public interface ModalidadeEntrega extends Catalogavel {

    /** Se esta modalidade atende este pedido (peso, valor, o que for). */
    default boolean atende(Pedido pedido) {
        return true;
    }

    Entrega calcular(Pedido pedido);
}
