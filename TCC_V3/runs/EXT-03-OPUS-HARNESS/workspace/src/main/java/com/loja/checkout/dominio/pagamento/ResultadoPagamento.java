package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/** O que a forma de pagamento define: o valor final e o valor de cada parcela. */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
