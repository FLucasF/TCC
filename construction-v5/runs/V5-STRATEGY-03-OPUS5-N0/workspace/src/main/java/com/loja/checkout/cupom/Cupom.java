package com.loja.checkout.cupom;

import java.math.BigDecimal;

/**
 * Uma promocao. Cada cupom novo do marketing e uma implementacao anotada com
 * @Component, que entra sozinha no catalogo.
 */
public interface Cupom {

    /** Codigo digitado pelo cliente, sempre em letras maiusculas. */
    String codigo();

    /** Se o pedido cumpre a condicao da promocao. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Desconto do cupom, ja arredondado em centavos. */
    BigDecimal desconto(ContextoCupom contexto);
}
