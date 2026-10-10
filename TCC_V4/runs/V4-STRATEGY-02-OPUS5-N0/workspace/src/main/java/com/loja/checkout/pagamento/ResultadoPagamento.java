package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Valor final e valor de cada parcela depois do ajuste da forma de pagamento. */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
