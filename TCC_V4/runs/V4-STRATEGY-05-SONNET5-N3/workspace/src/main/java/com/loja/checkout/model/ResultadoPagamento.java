package com.loja.checkout.model;

import java.math.BigDecimal;

public record ResultadoPagamento(
        BigDecimal ajuste,
        BigDecimal totalFinal,
        BigDecimal valorParcela
) {
}
