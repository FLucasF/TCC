package com.loja.checkout.cupom;

import com.loja.checkout.catalogo.Identificado;
import java.math.BigDecimal;

/**
 * Uma promocao. Cada cupom diz quando vale e quanto desconta; promocao nova
 * e uma classe nova.
 */
public interface Cupom extends Identificado {

    /** Se o pedido cumpre a condicao da promocao. */
    default boolean aplicavel(ContextoCupom pedido) {
        return true;
    }

    BigDecimal desconto(ContextoCupom pedido);
}
