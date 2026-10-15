package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Codificavel;
import com.loja.checkout.dominio.Pedido;

/** Uma forma de o cliente receber o pedido. */
public interface ModalidadeEntrega extends Codificavel {

    /** Se a modalidade atende este pedido (ex.: motoboy só até 5 kg). */
    default boolean atende(Pedido pedido) {
        return true;
    }

    Entrega apurar(Pedido pedido);
}
