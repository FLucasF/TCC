package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Resultado da forma de pagamento sobre o total do pedido.
 *
 * @param parcelas        em quantas vezes
 * @param valorParcela    valor de cada parcela
 * @param totalFinal      valor final da compra
 * @param ajustePagamento valor final menos o total do pedido
 *                        (negativo = desconto, positivo = tarifa/juros, zero = não muda nada)
 */
public record ResultadoPagamento(int parcelas, BigDecimal valorParcela, BigDecimal totalFinal,
                                 BigDecimal ajustePagamento) {
}
