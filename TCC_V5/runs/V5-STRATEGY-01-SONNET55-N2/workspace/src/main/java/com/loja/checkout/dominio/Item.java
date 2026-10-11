package com.loja.checkout.dominio;

import java.math.BigDecimal;

public record Item(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public BigDecimal valor() {
        return Dinheiro.arredondar(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    public BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
