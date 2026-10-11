package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

/**
 * Uma forma de entrega. Cada modalidade tem seu jeito de cobrar, seu prazo e
 * suas limitações — é o ponto de extensão para as transportadoras novas:
 * basta uma nova implementação registrada como componente.
 */
public interface ModalidadeEntrega {

    /** Código exatamente como o site envia (ex.: "EXPRESSA"). */
    String codigo();

    /** Prazo de entrega em dias. */
    int prazoDias();

    /** Se a modalidade atende um pedido com este peso (kg). */
    boolean disponivel(BigDecimal pesoKg);

    /** Frete para este peso (kg), já em centavos. */
    BigDecimal frete(BigDecimal pesoKg);
}
