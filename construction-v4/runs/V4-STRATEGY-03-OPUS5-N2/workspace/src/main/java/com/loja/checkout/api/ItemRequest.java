package com.loja.checkout.api;

import java.math.BigDecimal;

/** Uma linha do carrinho como o site envia. */
public record ItemRequest(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
