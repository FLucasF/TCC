package com.loja.checkout.dominio;

import java.math.BigDecimal;

public record Item(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public boolean valido() {
        return nome != null
                && precoUnitario != null && precoUnitario.signum() > 0
                && quantidade > 0
                && pesoKg != null && pesoKg.signum() > 0;
    }

    public BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
