package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Tudo que um cupom pode precisar olhar para decidir o desconto. */
public record ContextoCupom(Pedido pedido, BigDecimal subtotalProdutos, BigDecimal frete) {
}
