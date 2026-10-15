package com.loja.checkout.payment;

import java.math.BigDecimal;

public record PaymentResult(BigDecimal totalFinal, BigDecimal valorParcela) {
}
