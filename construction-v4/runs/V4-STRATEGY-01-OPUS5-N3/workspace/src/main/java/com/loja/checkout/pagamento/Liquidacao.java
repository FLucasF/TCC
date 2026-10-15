package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Como a forma de pagamento fecha a conta: o valor final e cada parcela. */
public record Liquidacao(BigDecimal totalFinal, BigDecimal valorParcela) {
}
