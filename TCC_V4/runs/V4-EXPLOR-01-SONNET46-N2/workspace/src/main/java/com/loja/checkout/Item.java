package com.loja.checkout;

import java.math.BigDecimal;

public record Item(
        String nome,
        BigDecimal precoUnitario,
        Integer quantidade,
        BigDecimal pesoKg) {
}
