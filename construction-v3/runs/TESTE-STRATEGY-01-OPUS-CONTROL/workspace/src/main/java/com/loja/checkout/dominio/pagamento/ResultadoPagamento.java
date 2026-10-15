package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/**
 * Efeito da forma de pagamento sobre o total do pedido.
 *
 * @param totalFinal    valor que o cliente paga no fim
 * @param valorParcela  valor de cada parcela
 */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
