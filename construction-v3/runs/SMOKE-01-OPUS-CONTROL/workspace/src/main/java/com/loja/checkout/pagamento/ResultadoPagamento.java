package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** O que a forma de pagamento devolve: quanto o cliente paga no fim e o valor de cada parcela. */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
