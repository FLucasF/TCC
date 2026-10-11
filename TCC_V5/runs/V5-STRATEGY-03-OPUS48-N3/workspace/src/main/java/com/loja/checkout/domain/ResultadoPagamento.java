package com.loja.checkout.domain;

import java.math.BigDecimal;

/** Resultado do pagamento: valor final do pedido e valor de cada parcela. */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
