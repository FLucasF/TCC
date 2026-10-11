package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/**
 * O que a forma de pagamento produz a partir do total do pedido: o valor final
 * e o valor de cada parcela. O ajuste de pagamento (final menos total) é
 * derivado disso por quem chama.
 */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
