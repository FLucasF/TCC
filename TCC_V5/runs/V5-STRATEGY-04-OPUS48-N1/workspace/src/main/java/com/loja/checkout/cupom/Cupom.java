package com.loja.checkout.cupom;

import java.math.BigDecimal;

/**
 * Uma promoção. O marketing inventa o tempo todo: cada cupom é uma classe própria.
 * Para adicionar um cupom, basta criar uma classe que implemente esta interface.
 */
public interface Cupom {

    String codigo();

    /** Se o pedido cumpre a condição do cupom (ex.: MENOS50 só a partir de R$ 300). */
    default boolean aplicavel(CupomContexto ctx) {
        return true;
    }

    BigDecimal desconto(CupomContexto ctx);
}
