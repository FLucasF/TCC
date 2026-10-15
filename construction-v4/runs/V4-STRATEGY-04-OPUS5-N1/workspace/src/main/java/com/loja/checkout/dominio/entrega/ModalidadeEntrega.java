package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/** Uma opcao de entrega: seu jeito de cobrar, seu prazo e suas limitacoes. */
public interface ModalidadeEntrega {

    String codigo();

    /** Prazo de entrega, em dias. */
    int prazoDias();

    /** Se a opcao atende este pedido. */
    boolean atende(Pedido pedido);

    /** Frete cobrado por esta opcao, arredondado para centavos. */
    BigDecimal frete(Pedido pedido);
}
