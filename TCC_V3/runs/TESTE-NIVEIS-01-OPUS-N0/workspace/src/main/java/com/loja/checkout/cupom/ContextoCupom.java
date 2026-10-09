package com.loja.checkout.cupom;

import com.loja.checkout.pedido.Pedido;
import java.math.BigDecimal;

/** O que um cupom pode olhar para decidir o desconto: o pedido e o frete ja calculado. */
public record ContextoCupom(Pedido pedido, BigDecimal frete) {

    public BigDecimal subtotalProdutos() {
        return pedido.subtotalProdutos();
    }
}
