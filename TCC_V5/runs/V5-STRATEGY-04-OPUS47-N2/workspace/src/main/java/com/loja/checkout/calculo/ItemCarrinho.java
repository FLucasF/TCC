package com.loja.checkout.calculo;

import java.math.BigDecimal;

public record ItemCarrinho(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public BigDecimal subtotalItem() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal pesoItem() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
