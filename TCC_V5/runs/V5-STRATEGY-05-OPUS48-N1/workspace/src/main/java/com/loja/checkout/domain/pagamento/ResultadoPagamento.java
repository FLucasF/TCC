package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

/**
 * O que a forma de pagamento produz: o valor final da compra e quanto fica
 * cada parcela. O ajuste (valor final menos o total do pedido) o servico
 * calcula a partir daqui.
 */
public record ResultadoPagamento(BigDecimal valorFinal, BigDecimal valorParcela) {
}
