package com.loja.checkout.aplicacao;

import java.math.BigDecimal;

public record ItemSolicitado(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
