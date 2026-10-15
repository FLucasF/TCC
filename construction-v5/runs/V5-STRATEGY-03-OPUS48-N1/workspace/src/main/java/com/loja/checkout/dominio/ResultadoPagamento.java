package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * O que a forma de pagamento produz sobre o total do pedido: o valor final e
 * o valor de cada parcela, ambos ja em centavos.
 */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
