package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** O que uma forma de pagamento produz: o valor final e o valor de cada parcela. */
public record ResultadoPagamento(BigDecimal valorFinal, BigDecimal valorParcela) {
}
