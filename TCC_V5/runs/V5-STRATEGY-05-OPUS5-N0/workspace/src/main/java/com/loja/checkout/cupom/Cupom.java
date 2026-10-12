package com.loja.checkout.cupom;

import java.math.BigDecimal;

/**
 * Uma promocao. Para o marketing lancar um cupom novo basta criar uma classe
 * que implemente esta interface e marca-la com @Component.
 */
public interface Cupom {

    /** Codigo digitado pelo cliente, sempre em maiusculas. */
    String codigo();

    /** Se o pedido cumpre a condicao da promocao. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Desconto do cupom, ja arredondado em centavos. */
    BigDecimal calcularDesconto(ContextoCupom contexto);
}
