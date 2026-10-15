package com.loja.checkout.web.dto;

import java.math.BigDecimal;

public record ItemDto(
        String nome,
        BigDecimal precoUnitario,
        Integer quantidade,
        BigDecimal pesoKg
) {}
