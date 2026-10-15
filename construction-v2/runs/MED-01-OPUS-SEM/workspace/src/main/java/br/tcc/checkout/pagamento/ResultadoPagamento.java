package br.tcc.checkout.pagamento;

import java.math.BigDecimal;

/** O que a forma de pagamento faz com o total do pedido. */
public record ResultadoPagamento(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {
}
