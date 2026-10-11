package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * Uma promoção. Cada código novo entra como uma implementação com sua própria
 * conta de desconto e sua própria condição de uso.
 */
public interface Cupom {

    String codigo();

    /** O frete já calculado entra porque FRETEGRATIS desconta exatamente ele. */
    BigDecimal desconto(Pedido pedido, BigDecimal frete);

    /** Se o pedido cumpre a condição da promoção. */
    default boolean aplicavelA(Pedido pedido) {
        return true;
    }
}
