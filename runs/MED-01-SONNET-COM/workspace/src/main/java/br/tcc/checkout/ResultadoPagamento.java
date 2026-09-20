package br.tcc.checkout;

import java.math.BigDecimal;

record ResultadoPagamento(BigDecimal ajuste, BigDecimal totalFinal, BigDecimal valorParcela) {
}
