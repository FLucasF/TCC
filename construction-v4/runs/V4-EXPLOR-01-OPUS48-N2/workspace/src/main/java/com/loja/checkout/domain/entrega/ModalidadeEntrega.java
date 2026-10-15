package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

/**
 * Uma forma de entrega. Cada modalidade tem seu jeito de cobrar o frete, seu
 * prazo e suas limitações, então cada uma mora numa implementação própria.
 * Entra modalidade nova adicionando uma nova implementação.
 */
public interface ModalidadeEntrega {

    String codigo();

    /** Frete antes de qualquer benefício do clube, dado o peso do pedido. */
    BigDecimal frete(BigDecimal pesoKg);

    int prazoDias();

    /** Se a modalidade atende um pedido com este peso. Por padrão, atende. */
    default boolean atende(BigDecimal pesoKg) {
        return true;
    }
}
