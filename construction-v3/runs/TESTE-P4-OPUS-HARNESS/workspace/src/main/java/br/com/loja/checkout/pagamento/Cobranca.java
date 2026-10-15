package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** O que o cliente paga na forma escolhida: o valor final e o valor de cada parcela. */
public record Cobranca(BigDecimal valorFinal, BigDecimal valorParcela) {
}
