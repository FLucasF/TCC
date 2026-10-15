package com.loja.checkout.domain;

import java.math.BigDecimal;

public record ResultadoPagamento(
        BigDecimal totalFinal,
        BigDecimal ajustePagamento,
        int parcelas,
        BigDecimal valorParcela) {}
