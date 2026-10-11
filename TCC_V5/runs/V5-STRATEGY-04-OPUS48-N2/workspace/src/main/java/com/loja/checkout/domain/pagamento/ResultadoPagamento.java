package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

/** O que a forma de pagamento produz: o valor final e o valor de cada parcela. */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
