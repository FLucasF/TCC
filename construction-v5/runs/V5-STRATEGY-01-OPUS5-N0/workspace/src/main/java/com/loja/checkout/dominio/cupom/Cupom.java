package com.loja.checkout.dominio.cupom;

import java.math.BigDecimal;

/**
 * Uma promocao. Para o marketing lancar um cupom novo, basta uma implementacao
 * desta interface anotada com @Component.
 */
public interface Cupom {

    /** Codigo digitado pelo cliente, sempre em letras maiusculas. */
    String codigo();

    /** Diz se o pedido cumpre a condicao da promocao. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Valor do desconto, arredondado para centavos. */
    BigDecimal desconto(ContextoCupom contexto);
}
