package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * O que um cupom pode olhar para decidir: os itens, a soma dos produtos e o
 * frete ja calculado. A assinatura atende o cupom mais exigente.
 */
public record ContextoCupom(Pedido pedido, BigDecimal subtotalProdutos, BigDecimal frete) {
}
