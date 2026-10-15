package com.loja.checkout.dto;

import java.math.BigDecimal;

public record ItemRequest(
    String nome,
    BigDecimal precoUnitario,
    int quantidade,
    BigDecimal pesoKg
) {}
