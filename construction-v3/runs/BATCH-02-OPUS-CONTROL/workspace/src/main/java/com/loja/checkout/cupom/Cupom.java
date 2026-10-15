package com.loja.checkout.cupom;

import java.math.BigDecimal;

/**
 * Uma promocao. Cupom novo do marketing = uma classe nova com @Component
 * implementando esta interface.
 */
public interface Cupom {

    /** Codigo digitado pelo cliente, sempre em letras maiusculas. */
    String codigo();

    /** Se o pedido cumpre a condicao da promocao. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Valor do desconto, sem arredondar. */
    BigDecimal desconto(ContextoCupom contexto);
}
