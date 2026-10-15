package com.loja.checkout.cupom;

import java.math.BigDecimal;

/**
 * Uma promocao da loja. Para o marketing lancar um cupom novo, basta uma classe nova
 * que implemente esta interface e marcar com @Component: ela entra sozinha no {@link Cupons}.
 */
public interface Cupom {

    /** Codigo do cupom, sempre em letras maiusculas. */
    String codigo();

    /** Se o pedido cumpre a condicao da promocao. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Desconto do cupom, antes de arredondar. */
    BigDecimal desconto(ContextoCupom contexto);
}
