package com.loja.checkout.web.dto;

import java.math.BigDecimal;

public record ItemRequestDTO(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
