package com.loja.resumo.domain;

import java.math.BigDecimal;

public record ResultadoPagamento(BigDecimal ajustePagamento, BigDecimal totalFinal, BigDecimal valorParcela) {
}
