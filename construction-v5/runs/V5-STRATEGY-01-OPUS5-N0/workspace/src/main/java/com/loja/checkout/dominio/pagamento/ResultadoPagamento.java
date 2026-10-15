package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/** Valor final da compra e valor de cada parcela, ja em centavos. */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
