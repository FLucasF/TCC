package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Identificavel;
import com.loja.checkout.dominio.Pedido;

/** Uma forma de o cliente receber o pedido. Uma implementacao por modalidade. */
public interface ModalidadeEntrega extends Identificavel {

    /** Se a modalidade atende este pedido (peso, tamanho, destino...). */
    boolean atende(Pedido pedido);

    Entrega calcular(Pedido pedido);
}
