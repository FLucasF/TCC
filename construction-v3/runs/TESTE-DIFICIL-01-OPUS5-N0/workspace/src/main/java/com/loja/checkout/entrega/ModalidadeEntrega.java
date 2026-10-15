package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Identificavel;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/**
 * Uma forma de o cliente receber o pedido: tem seu jeito de cobrar, seu prazo
 * e suas limitacoes. Transportadora nova = um bean novo implementando esta interface.
 */
public interface ModalidadeEntrega extends Identificavel {

    /** Se esta modalidade atende este pedido (peso, tamanho, destino...). */
    default boolean atende(Pedido pedido) {
        return true;
    }

    /** Quanto custa o frete para este pedido. */
    BigDecimal frete(Pedido pedido);

    /** Prazo de entrega, em dias. */
    int prazoDias();

    /** Se o pedido viaja e portanto leva seguro de envio. */
    default boolean temSeguro() {
        return true;
    }
}
