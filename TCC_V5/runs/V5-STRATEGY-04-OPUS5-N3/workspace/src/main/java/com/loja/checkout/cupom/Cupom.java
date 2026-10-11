package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/** Um cupom: a condição que o pedido precisa cumprir e o desconto que dá. */
public interface Cupom {

    String codigo();

    /** Se o pedido cumpre a condição do cupom (ex.: valor mínimo em produtos). */
    default boolean aplicavel(Pedido pedido) {
        return true;
    }

    /** O frete entra porque FRETEGRATIS desconta exatamente o frete cobrado. */
    BigDecimal desconto(Pedido pedido, BigDecimal frete);
}
