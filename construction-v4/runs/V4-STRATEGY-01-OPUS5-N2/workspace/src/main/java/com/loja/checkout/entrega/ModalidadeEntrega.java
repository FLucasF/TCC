package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Identificavel;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/**
 * Uma forma de o cliente receber o pedido. Cada parceria nova de entrega e' uma
 * implementacao desta interface: traz seu jeito de cobrar, seu prazo e, se
 * tiver, sua limitacao.
 */
public interface ModalidadeEntrega extends Identificavel {

    /** Quanto custa o frete deste pedido por esta modalidade. */
    BigDecimal frete(Pedido pedido);

    /** O prazo de entrega, em dias. */
    int prazoDias();

    /** Se esta modalidade atende este pedido. Por padrao, atende qualquer um. */
    default boolean atende(Pedido pedido) {
        return true;
    }
}
