package com.loja.checkout.web.dto;

import java.math.BigDecimal;

public record ItemRequest(String nome, BigDecimal precoUnitario, Integer quantidade, Double pesoKg) {
}
