package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Valor que o cliente vai pagar e valor de cada parcela, em centavos. */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
