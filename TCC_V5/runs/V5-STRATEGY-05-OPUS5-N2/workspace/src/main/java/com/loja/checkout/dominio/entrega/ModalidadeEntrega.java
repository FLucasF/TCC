package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Identificavel;
import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * Uma opcao de entrega. Cada parceria nova entra como uma implementacao
 * propria, com seu jeito de cobrar, seu prazo e suas limitacoes.
 */
public interface ModalidadeEntrega extends Identificavel {

    /** Se esta opcao atende este pedido (peso, itens, etc.). */
    default boolean atende(Pedido pedido) {
        return true;
    }

    BigDecimal frete(Pedido pedido);

    int prazoDias();
}
