package com.loja.checkout.dominio;

import java.math.BigDecimal;

public record Item(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public boolean valido() {
        return nome != null && !nome.isBlank()
                && precoUnitario != null && precoUnitario.compareTo(BigDecimal.ZERO) > 0
                && quantidade > 0
                && pesoKg != null && pesoKg.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal pesoTotal() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
