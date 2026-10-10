package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** O que a forma de pagamento define: o valor final e o valor de cada parcela. */
public record Pagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
