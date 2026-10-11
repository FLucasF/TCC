package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Um item do carrinho como o site envia. */
public record ItemInformado(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
