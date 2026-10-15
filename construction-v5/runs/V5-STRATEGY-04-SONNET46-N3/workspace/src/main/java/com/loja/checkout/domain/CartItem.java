package com.loja.checkout.domain;

import java.math.BigDecimal;

public record CartItem(
        String nome,
        BigDecimal precoUnitario,
        Integer quantidade,
        BigDecimal pesoKg
) {}
