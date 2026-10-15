package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Um produto do carrinho, como o site envia (qualquer campo pode faltar). */
public record ItemSolicitado(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
