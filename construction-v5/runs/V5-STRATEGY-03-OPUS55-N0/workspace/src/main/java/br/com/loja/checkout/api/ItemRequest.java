package br.com.loja.checkout.api;

import java.math.BigDecimal;

/** Quantidade chega como número para que valores fracionados sejam recusados, e não truncados. */
public record ItemRequest(String nome, BigDecimal precoUnitario, BigDecimal quantidade, BigDecimal pesoKg) {
}
