package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Valor final do pedido e valor de cada parcela, segundo a forma de pagamento. */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
