package com.loja.checkout.resumo.entrega;

import java.math.BigDecimal;

/**
 * Uma opção de entrega: seu jeito de cobrar, seu prazo e suas limitações.
 * Para incluir uma transportadora nova, basta uma implementação desta
 * interface registrada em {@link Entregas}.
 */
public interface ModalidadeEntrega {

    BigDecimal frete(BigDecimal pesoKg);

    int prazoDias();

    /** Se a opção atende um pedido com este peso. */
    default boolean atende(BigDecimal pesoKg) {
        return true;
    }
}
