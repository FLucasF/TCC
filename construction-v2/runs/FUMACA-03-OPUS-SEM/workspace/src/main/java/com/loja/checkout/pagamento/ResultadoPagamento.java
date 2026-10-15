package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Valor final cobrado do cliente e valor de cada parcela, ja em centavos. */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
