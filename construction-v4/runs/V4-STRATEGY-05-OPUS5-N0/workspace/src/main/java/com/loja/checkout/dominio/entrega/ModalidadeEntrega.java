package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

/**
 * Uma forma de entrega da loja. Para incluir uma transportadora nova, basta
 * criar uma classe nova que implemente esta interface e marcar com @Component:
 * ela entra sozinha no catalogo.
 */
public interface ModalidadeEntrega {

    /** Codigo usado pelo site (ex.: EXPRESSA). */
    String codigo();

    /** Prazo de entrega em dias. */
    int prazoEntregaDias();

    /** Frete cobrado para o peso informado, arredondado para centavos. */
    BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    /** Se esta modalidade atende um pedido com esse peso. */
    default boolean atende(BigDecimal pesoTotalKg) {
        return true;
    }
}
