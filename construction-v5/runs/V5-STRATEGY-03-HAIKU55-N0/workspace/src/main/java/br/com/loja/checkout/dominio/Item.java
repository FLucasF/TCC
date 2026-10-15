package br.com.loja.checkout.dominio;

import java.math.BigDecimal;

public record Item(BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {
}
