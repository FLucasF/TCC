package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** O que a forma de pagamento devolve: o valor final e como ele fica dividido. */
public record ResultadoPagamento(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {
}
