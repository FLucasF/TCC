package com.loja.checkout.dominio;

import java.math.BigDecimal;

public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {

    public static ResultadoPagamento aVista(BigDecimal totalFinal) {
        BigDecimal valor = Dinheiro.arredondar(totalFinal);
        return new ResultadoPagamento(valor, valor);
    }

    public BigDecimal ajusteSobre(BigDecimal totalPedido) {
        return Dinheiro.arredondar(totalFinal.subtract(totalPedido));
    }
}
