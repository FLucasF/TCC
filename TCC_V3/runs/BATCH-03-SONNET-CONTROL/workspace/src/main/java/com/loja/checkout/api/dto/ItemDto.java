package com.loja.checkout.api.dto;

import java.math.BigDecimal;

public record ItemDto(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
