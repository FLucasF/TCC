package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Resultado da forma de pagamento sobre o total do pedido.
 *
 * @param parcelas      número de parcelas
 * @param valorParcela  valor de cada parcela
 * @param totalFinal    valor final da compra
 * @param ajuste        valor final menos o total do pedido (negativo = desconto,
 *                      positivo = tarifa ou juros, zero = não muda nada)
 */
public record PagamentoResultado(
        int parcelas,
        BigDecimal valorParcela,
        BigDecimal totalFinal,
        BigDecimal ajuste
) {
}
