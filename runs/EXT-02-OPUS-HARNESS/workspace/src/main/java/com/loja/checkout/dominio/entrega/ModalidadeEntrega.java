package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Codificavel;
import com.loja.checkout.dominio.Pedido;

public interface ModalidadeEntrega extends Codificavel {

    /** Se a modalidade atende este pedido (peso, tamanho, restrições próprias). */
    default boolean atende(Pedido pedido) {
        return true;
    }

    Frete calcular(Pedido pedido);
}
