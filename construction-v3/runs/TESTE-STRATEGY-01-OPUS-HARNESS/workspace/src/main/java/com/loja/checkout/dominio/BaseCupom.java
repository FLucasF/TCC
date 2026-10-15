package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Tudo que um cupom pode precisar olhar para decidir se vale e quanto abate:
 * os itens, o valor dos produtos e o frete ja cotado.
 */
public record BaseCupom(Carrinho carrinho, BigDecimal subtotalProdutos, BigDecimal frete) {
}
