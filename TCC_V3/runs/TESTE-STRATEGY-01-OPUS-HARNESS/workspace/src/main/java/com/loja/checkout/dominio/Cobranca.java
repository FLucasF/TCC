package com.loja.checkout.dominio;

import java.math.BigDecimal;

public record Cobranca(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {

    public BigDecimal ajuste(BigDecimal totalPedido) {
        return Dinheiro.centavos(totalFinal.subtract(totalPedido));
    }
}
