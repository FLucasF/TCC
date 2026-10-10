package com.loja.checkout.entrega;

import java.math.BigDecimal;

/**
 * Uma opção de entrega. Para oferecer uma transportadora nova, basta criar uma
 * classe que implemente esta interface e marcá-la com {@code @Component}: ela
 * passa a ser reconhecida pelo serviço sem nenhuma outra alteração.
 */
public interface ModalidadeEntrega {

    /** Código usado pelo site, ex.: {@code EXPRESSA}. */
    String codigo();

    /** Prazo de entrega em dias. */
    int prazoDias();

    /** Frete cobrado para o pedido, arredondado em centavos. */
    BigDecimal frete(ContextoEntrega contexto);

    /** Se esta opção atende o pedido (peso, região, valor...). */
    default boolean atende(ContextoEntrega contexto) {
        return true;
    }
}
