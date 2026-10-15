package com.loja.checkout.web;

import java.math.BigDecimal;

public record ItemRequisicao(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
