package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Identificado;

import java.math.BigDecimal;

/** Uma promoção: quanto ela desconta e em que condição ela vale. */
public interface Cupom extends Identificado {

    BigDecimal desconto(ContextoCupom contexto);

    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }
}
