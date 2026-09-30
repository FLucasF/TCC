package com.loja.checkout.enums;

import java.math.BigDecimal;

public record ResultadoPagamento(BigDecimal ajuste, BigDecimal totalFinal, BigDecimal valorParcela) {
}
