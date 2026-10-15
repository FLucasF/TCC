package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** O que a forma de pagamento devolve: valor final, parcelas e valor de cada parcela. */
public record ResultadoPagamento(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {
}
