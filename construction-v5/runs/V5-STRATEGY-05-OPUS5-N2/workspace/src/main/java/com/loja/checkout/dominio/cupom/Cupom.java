package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Identificavel;

import java.math.BigDecimal;

/** Uma promocao do marketing. Cada cupom novo entra como uma implementacao propria. */
public interface Cupom extends Identificavel {

    /** Se o pedido cumpre a condicao da promocao. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    BigDecimal desconto(ContextoCupom contexto);
}
