package com.loja.checkout.model;

import java.math.BigDecimal;

public record Item(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {

    public boolean valido() {
        return nome != null
                && precoUnitario != null && precoUnitario.signum() > 0
                && quantidade != null && quantidade > 0
                && pesoKg != null && pesoKg.signum() > 0;
    }

    public BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal pesoTotal() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
