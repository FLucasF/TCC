package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * O que a forma de pagamento produz a partir do total do pedido.
 *
 * @param totalFinal   valor final da compra
 * @param valorParcela valor de cada parcela
 */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
