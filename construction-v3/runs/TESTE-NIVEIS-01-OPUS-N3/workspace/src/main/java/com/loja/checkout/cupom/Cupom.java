package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Codificavel;

import java.math.BigDecimal;

/** Uma promoção do marketing. */
public interface Cupom extends Codificavel {

    /** Se o pedido cumpre a condição do cupom. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    BigDecimal desconto(ContextoCupom contexto);
}
