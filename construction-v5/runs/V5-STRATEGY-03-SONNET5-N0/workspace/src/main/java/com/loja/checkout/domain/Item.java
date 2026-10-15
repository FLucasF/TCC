package com.loja.checkout.domain;

import java.math.BigDecimal;

public record Item(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public boolean valido() {
        return nome != null && !nome.isBlank()
                && precoUnitario != null && precoUnitario.signum() > 0
                && quantidade > 0
                && pesoKg != null && pesoKg.signum() > 0;
    }

    public BigDecimal totalPreco() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal totalPeso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
