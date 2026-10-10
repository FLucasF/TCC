package com.loja.checkout.dto;

import java.math.BigDecimal;

public record ResultadoPagamento(BigDecimal ajuste, BigDecimal totalFinal, BigDecimal valorParcela) {
}
