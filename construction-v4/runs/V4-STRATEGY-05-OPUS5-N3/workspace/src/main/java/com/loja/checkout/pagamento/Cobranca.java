package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** O que a forma de pagamento cobra: o valor final e o valor de cada parcela. */
public record Cobranca(BigDecimal totalFinal, BigDecimal valorParcela) {
}
