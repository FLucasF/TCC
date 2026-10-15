package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Um produto do carrinho, com a quantidade que o cliente colocou. */
public record Item(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public BigDecimal valorTotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal pesoTotalKg() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
