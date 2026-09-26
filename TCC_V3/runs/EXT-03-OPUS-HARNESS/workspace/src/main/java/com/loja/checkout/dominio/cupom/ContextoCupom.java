package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/** O que um cupom pode olhar para decidir: o carrinho e o frete ja calculado. */
public record ContextoCupom(Pedido pedido, BigDecimal frete) {

    public BigDecimal subtotal() {
        return pedido.subtotal();
    }
}
