package com.loja.checkout.domain;

import java.math.BigDecimal;

public record ResultadoPagamento(BigDecimal ajuste, BigDecimal totalFinal, BigDecimal valorParcela) {
}
