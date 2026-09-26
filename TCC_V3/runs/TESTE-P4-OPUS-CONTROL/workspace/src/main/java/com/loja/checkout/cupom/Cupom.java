package com.loja.checkout.cupom;

import java.math.BigDecimal;

/**
 * Uma promocao do marketing. Para criar uma nova basta uma implementacao
 * anotada com {@code @Component}.
 */
public interface Cupom {

    /** Codigo digitado pelo cliente, sempre em maiusculas. */
    String codigo();

    /** Se o pedido cumpre a condicao da promocao. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Valor do desconto, em centavos arredondados. */
    BigDecimal desconto(ContextoCupom contexto);
}
