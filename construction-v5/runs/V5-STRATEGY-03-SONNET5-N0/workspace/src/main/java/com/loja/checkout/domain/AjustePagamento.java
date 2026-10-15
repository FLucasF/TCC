package com.loja.checkout.domain;

import java.math.BigDecimal;

public record AjustePagamento(BigDecimal ajuste, BigDecimal totalFinal, BigDecimal valorParcela) {
}
