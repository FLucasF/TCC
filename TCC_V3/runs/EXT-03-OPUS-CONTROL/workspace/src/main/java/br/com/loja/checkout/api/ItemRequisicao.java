package br.com.loja.checkout.api;

import java.math.BigDecimal;

/** Um item do carrinho como o site manda (ainda sem validacao). */
public record ItemRequisicao(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
