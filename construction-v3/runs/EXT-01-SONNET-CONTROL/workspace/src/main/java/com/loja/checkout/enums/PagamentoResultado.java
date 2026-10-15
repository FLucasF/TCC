package com.loja.checkout.enums;

import java.math.BigDecimal;

public record PagamentoResultado(BigDecimal ajuste, BigDecimal totalFinal, BigDecimal valorParcela) {
}
