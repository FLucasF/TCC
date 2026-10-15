package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * O que a forma de pagamento produz sobre o total do pedido: o valor final e
 * o valor de cada parcela. O ajuste (final − total) o serviço calcula depois,
 * num lugar só.
 */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
