package com.loja.checkout.cupom;

import java.math.BigDecimal;

/**
 * Uma promoção do marketing. Para criar um cupom novo, basta uma classe que
 * implemente esta interface marcada com {@code @Component}.
 */
public interface Cupom {

    /** Código digitado pelo cliente, sempre em letras maiúsculas. */
    String codigo();

    /** Desconto do cupom, arredondado em centavos. */
    BigDecimal desconto(ContextoCupom contexto);

    /** Se o pedido cumpre a condição da promoção. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }
}
