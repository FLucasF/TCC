package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * Uma opcao de entrega. Cada parceria nova entra como uma implementacao propria,
 * registrada no catalogo como componente.
 */
public interface ModalidadeEntrega {

    String codigo();

    /** Se a opcao atende este pedido (ex.: limite de peso). */
    default boolean atende(Pedido pedido) {
        return true;
    }

    BigDecimal custo(Pedido pedido);

    int prazoDias();
}
