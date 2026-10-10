package com.loja.checkout.model;

import java.math.BigDecimal;

public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela, int parcelas) {
}
