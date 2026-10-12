package com.loja.checkout.payment;

import java.math.BigDecimal;

public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
