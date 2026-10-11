package com.loja.checkout.calculo;

import java.math.BigDecimal;

public record ItemCarrinho(BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal pesoTotal() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
