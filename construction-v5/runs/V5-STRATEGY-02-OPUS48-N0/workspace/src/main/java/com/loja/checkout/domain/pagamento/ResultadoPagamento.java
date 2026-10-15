package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

/**
 * Resultado da forma de pagamento sobre o total do pedido.
 *
 * @param parcelas     em quantas vezes
 * @param valorParcela valor de cada parcela
 * @param totalFinal   valor final do pedido
 * @param ajuste       valor final menos o total do pedido (negativo = desconto,
 *                     positivo = tarifa/juros, zero = nao muda nada)
 */
public record ResultadoPagamento(
        int parcelas,
        BigDecimal valorParcela,
        BigDecimal totalFinal,
        BigDecimal ajuste) {
}
