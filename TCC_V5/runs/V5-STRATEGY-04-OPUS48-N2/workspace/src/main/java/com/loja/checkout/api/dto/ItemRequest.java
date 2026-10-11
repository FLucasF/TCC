package com.loja.checkout.api.dto;

import java.math.BigDecimal;

/** Um produto do carrinho, como o site envia. Campos podem vir ausentes. */
public record ItemRequest(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
