package com.loja.checkout.cupom;

import com.loja.checkout.Carrinho;
import java.math.BigDecimal;

/** O que um cupom pode consultar: itens, subtotal dos produtos e frete do pedido. */
public record ContextoCupom(Carrinho carrinho, BigDecimal subtotalProdutos, BigDecimal frete) {
}
