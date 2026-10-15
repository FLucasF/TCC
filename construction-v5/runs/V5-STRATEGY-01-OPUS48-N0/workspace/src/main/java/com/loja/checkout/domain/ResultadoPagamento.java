package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Resultado do calculo da forma de pagamento sobre o total do pedido.
 *
 * @param totalFinal   valor final da compra
 * @param valorParcela valor de cada parcela
 */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
