package com.example.checkout.payment;

import java.math.BigDecimal;

public record PaymentResult(BigDecimal ajuste, BigDecimal totalFinal, BigDecimal valorParcela) {}
