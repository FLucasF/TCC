package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * O que a forma de pagamento produz a partir do total do pedido: o valor final
 * e o valor de cada parcela. O ajuste de pagamento (valorFinal - totalPedido)
 * é derivado disso lá no resumo.
 */
public record ResultadoPagamento(BigDecimal valorFinal, BigDecimal valorParcela) {
}
