package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/** Valor final do pedido e valor de cada parcela, ja arredondados. */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
