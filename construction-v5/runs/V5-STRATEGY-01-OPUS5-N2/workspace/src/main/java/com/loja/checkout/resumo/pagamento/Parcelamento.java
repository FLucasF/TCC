package com.loja.checkout.resumo.pagamento;

import java.math.BigDecimal;

/** O que a forma de pagamento define: o valor final e o valor de cada parcela. */
public record Parcelamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
