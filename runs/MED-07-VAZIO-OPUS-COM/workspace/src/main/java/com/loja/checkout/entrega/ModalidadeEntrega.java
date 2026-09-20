package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Codificado;
import com.loja.checkout.dominio.Entrega;
import com.loja.checkout.dominio.Pedido;

/**
 * Uma opcao de entrega. Cada opcao diz se atende o pedido, quanto cobra e em
 * quantos dias entrega. Para criar uma opcao nova, basta uma classe nova.
 */
public interface ModalidadeEntrega extends Codificado {

    default boolean atende(Pedido pedido) {
        return true;
    }

    Entrega calcular(Pedido pedido);
}
