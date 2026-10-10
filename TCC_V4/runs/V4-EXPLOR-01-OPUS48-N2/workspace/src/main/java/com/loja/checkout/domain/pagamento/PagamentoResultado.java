package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

/**
 * Como a forma de pagamento fecha o pedido: o valor final e o valor de cada
 * parcela. O ajuste (valor final menos total do pedido) é derivado disso.
 */
public record PagamentoResultado(BigDecimal totalFinal, BigDecimal valorParcela) {
}
