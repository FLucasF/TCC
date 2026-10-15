package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Codificavel;
import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * Uma forma de entrega. Cada parceria nova entra como uma implementacao propria,
 * com seu jeito de cobrar, seu prazo e sua limitacao.
 */
public interface ModalidadeEntrega extends Codificavel {

    BigDecimal frete(Pedido pedido);

    int prazoEntregaDias();

    /** Por padrao a modalidade atende qualquer pedido. */
    default boolean atende(Pedido pedido) {
        return true;
    }
}
