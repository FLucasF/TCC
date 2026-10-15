package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * O que a forma de pagamento produz a partir do total do pedido: o valor final
 * (já com desconto, tarifa ou juros) e o valor de cada parcela.
 */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
