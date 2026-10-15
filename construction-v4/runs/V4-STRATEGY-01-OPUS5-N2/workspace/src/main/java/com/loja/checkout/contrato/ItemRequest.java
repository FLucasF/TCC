package com.loja.checkout.contrato;

import java.math.BigDecimal;

/** Um produto do carrinho, exatamente como o site envia. */
public record ItemRequest(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
