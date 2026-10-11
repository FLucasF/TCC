package br.com.loja.checkout.cupom;

import br.com.loja.checkout.carrinho.Carrinho;

import java.math.BigDecimal;

/** Dados do pedido disponíveis para o cupom: carrinho, subtotal dos produtos e frete cobrado. */
public record ContextoCupom(Carrinho carrinho, BigDecimal subtotal, BigDecimal frete) {
}
