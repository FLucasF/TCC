package com.loja.online.checkout.dto;

import java.math.BigDecimal;

public record ItemCarrinhoDTO(
    String nome,
    BigDecimal precoUnitario,
    Integer quantidade,
    BigDecimal pesoKg
) {}
