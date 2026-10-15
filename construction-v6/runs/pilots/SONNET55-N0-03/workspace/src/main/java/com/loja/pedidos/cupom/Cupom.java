package com.loja.pedidos.cupom;

/** Um cupom. Nova promoção = novo bean. O teto de 30% é aplicado pela calculadora. */
public interface Cupom {

    String codigo();

    /** Desconto bruto, ou lança CUPOM_NAO_APLICAVEL se o pedido não cumpre a condição. */
    java.math.BigDecimal desconto(ContextoCupom contexto);
}
