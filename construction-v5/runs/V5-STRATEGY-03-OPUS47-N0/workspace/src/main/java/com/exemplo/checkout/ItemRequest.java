package com.exemplo.checkout;

import java.math.BigDecimal;

public record ItemRequest(
        String nome,
        BigDecimal precoUnitario,
        Integer quantidade,
        BigDecimal pesoKg
) {}
