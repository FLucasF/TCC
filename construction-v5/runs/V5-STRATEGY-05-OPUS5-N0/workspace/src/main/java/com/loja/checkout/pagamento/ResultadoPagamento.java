package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Efeito da forma de pagamento sobre o total do pedido.
 *
 * @param totalFinal   valor final cobrado do cliente
 * @param valorParcela valor de cada parcela
 * @param ajuste       totalFinal menos o total do pedido (negativo = desconto,
 *                     positivo = tarifa ou juros)
 */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela, BigDecimal ajuste) {
}
