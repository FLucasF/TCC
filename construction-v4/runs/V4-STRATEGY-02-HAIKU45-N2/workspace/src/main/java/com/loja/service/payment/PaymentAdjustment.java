package com.loja.service.payment;

import java.math.BigDecimal;

public record PaymentAdjustment(
    BigDecimal ajuste,
    Integer parcelas,
    BigDecimal valorParcela
) {}
