package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Resultado do cálculo da forma de pagamento.
 *
 * @param ajuste       valor final menos o total do pedido (negativo = desconto,
 *                     positivo = tarifa ou juros, zero = não muda nada)
 * @param totalFinal   valor final da compra
 * @param parcelas     número de parcelas efetivo
 * @param valorParcela valor de cada parcela
 */
public record ResultadoPagamento(BigDecimal ajuste, BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {
}
