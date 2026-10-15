package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** O que a forma de pagamento apura sobre o total do pedido. */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
