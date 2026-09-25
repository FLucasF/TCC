package com.loja.checkout.cupom;

import java.math.BigDecimal;

/**
 * Uma promocao do marketing.
 *
 * <p>Para criar um cupom novo basta implementar esta interface e anotar a classe com
 * {@code @Component}: ele passa a valer automaticamente.
 */
public interface Cupom {

    /** Codigo digitado pelo cliente, sempre em maiusculas (ex.: BEMVINDO10). */
    String codigo();

    /** Diz se o pedido cumpre as condicoes da promocao. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Valor do desconto, arredondado para centavos. */
    BigDecimal desconto(ContextoCupom contexto);
}
