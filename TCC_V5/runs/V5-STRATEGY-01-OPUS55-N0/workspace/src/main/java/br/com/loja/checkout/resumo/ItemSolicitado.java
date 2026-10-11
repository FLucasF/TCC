package br.com.loja.checkout.resumo;

import java.math.BigDecimal;

public record ItemSolicitado(String nome, BigDecimal precoUnitario, BigDecimal quantidade, BigDecimal pesoKg) {
}
