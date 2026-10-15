package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** O que a forma de pagamento define: o valor final e o valor de cada parcela. */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {

    public static ResultadoPagamento aVista(BigDecimal totalFinal) {
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
