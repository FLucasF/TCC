package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Efeito da forma de pagamento sobre o total do pedido.
 *
 * @param totalFinal   valor que o cliente paga
 * @param valorParcela valor de cada parcela
 */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
