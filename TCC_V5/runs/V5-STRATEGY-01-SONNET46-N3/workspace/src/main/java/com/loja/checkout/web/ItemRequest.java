package com.loja.checkout.web;

import java.math.BigDecimal;

public record ItemRequest(
        String nome,
        BigDecimal precoUnitario,
        Integer quantidade,
        BigDecimal pesoKg
) {}
