package com.loja.checkout.cupom;

import java.math.BigDecimal;

/**
 * Uma promocao do marketing. Para criar uma nova, basta uma classe com
 * @Component implementando esta interface; ela passa a valer automaticamente.
 */
public interface Cupom {

    /** Codigo digitado pelo cliente, sempre em maiusculas. */
    String codigo();

    /** Valor do desconto, ja em centavos. */
    BigDecimal desconto(ContextoCupom contexto);

    /** Condicoes da promocao (ex.: MENOS50 exige R$ 300,00 em produtos). */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }
}
