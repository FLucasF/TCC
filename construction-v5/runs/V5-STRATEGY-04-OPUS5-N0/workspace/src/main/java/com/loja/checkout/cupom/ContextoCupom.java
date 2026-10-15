package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * O que um cupom pode olhar para decidir o desconto: o carrinho, a soma dos produtos
 * e o frete já calculado (o FRETEGRATIS precisa dele).
 */
public record ContextoCupom(Pedido pedido, BigDecimal subtotalProdutos, BigDecimal frete) {
}
