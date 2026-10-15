package com.loja.checkout.api;

import java.math.BigDecimal;

public record ItemRequisicao(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
