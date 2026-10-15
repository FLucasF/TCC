package br.tcc.checkout.api;

import java.math.BigDecimal;

/** Item do carrinho como o site envia. */
public record ItemRequest(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
