package com.loja.resumo.api;

import java.math.BigDecimal;

public record ItemRequest(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
