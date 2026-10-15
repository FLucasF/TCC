package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Carrinho;

import java.math.BigDecimal;

/** Tudo que um cupom precisa saber para decidir o desconto. */
public record ContextoCupom(Carrinho carrinho, BigDecimal subtotalProdutos, BigDecimal frete) {
}
