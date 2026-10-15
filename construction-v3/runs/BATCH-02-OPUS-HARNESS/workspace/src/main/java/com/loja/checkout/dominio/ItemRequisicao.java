package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Item como chega do site, antes de ser validado. */
public record ItemRequisicao(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
