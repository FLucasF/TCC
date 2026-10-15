package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** O que a forma de pagamento devolve: o valor final e o valor de cada parcela. */
public record ValorCobrado(BigDecimal totalFinal, BigDecimal valorParcela) {

    /** À vista, a única parcela é o próprio valor final. */
    static ValorCobrado aVista(BigDecimal totalFinal) {
        BigDecimal valor = Dinheiro.centavos(totalFinal);
        return new ValorCobrado(valor, valor);
    }
}
