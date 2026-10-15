package com.loja.checkout.dominio;

import java.math.BigDecimal;

public record Item(String nome, BigDecimal precoUnitario, int quantidade, double pesoKg) {

    public boolean valido() {
        return precoUnitario != null
                && precoUnitario.signum() > 0
                && quantidade > 0
                && pesoKg > 0;
    }

    public BigDecimal totalItem() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public double pesoTotal() {
        return pesoKg * quantidade;
    }
}
