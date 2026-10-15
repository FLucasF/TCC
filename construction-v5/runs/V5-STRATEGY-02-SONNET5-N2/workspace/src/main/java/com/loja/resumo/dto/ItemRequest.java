package com.loja.resumo.dto;

import java.math.BigDecimal;

public record ItemRequest(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
