package br.com.loja.checkout.cupom;

import br.com.loja.checkout.carrinho.Carrinho;

import java.math.BigDecimal;

/** O que um cupom pode consultar para decidir se vale e quanto desconta. */
public record ContextoCupom(Carrinho carrinho, BigDecimal frete) {
}
