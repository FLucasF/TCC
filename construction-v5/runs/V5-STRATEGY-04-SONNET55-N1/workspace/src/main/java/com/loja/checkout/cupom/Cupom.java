package com.loja.checkout.cupom;

import com.loja.checkout.Identificavel;

/** Um cupom: quando vale para o pedido e quanto desconta. */
public interface Cupom extends Identificavel {
    boolean aplicavel(ContextoCupom pedido);

    java.math.BigDecimal desconto(ContextoCupom pedido);
}
