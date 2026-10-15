package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Resultado da forma de pagamento: valor final e valor de cada parcela. */
public record Cobranca(BigDecimal totalFinal, BigDecimal valorParcela) {
}
