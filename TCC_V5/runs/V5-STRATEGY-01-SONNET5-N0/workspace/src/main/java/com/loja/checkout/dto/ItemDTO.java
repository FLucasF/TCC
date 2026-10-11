package com.loja.checkout.dto;

import java.math.BigDecimal;

public record ItemDTO(
        String nome,
        BigDecimal precoUnitario,
        Integer quantidade,
        BigDecimal pesoKg
) {
}
