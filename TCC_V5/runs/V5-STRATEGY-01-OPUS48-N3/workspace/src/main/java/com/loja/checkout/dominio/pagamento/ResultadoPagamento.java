package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/**
 * O que a forma de pagamento produz: o valor final do pedido e o valor de cada
 * parcela. O ajuste (valor final menos total do pedido) é calculado pela
 * calculadora a partir daqui.
 */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
