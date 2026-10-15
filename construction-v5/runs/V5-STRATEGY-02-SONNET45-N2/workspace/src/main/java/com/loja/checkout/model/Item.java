package com.loja.checkout.model;

import java.math.BigDecimal;

public record Item(
    String nome,
    BigDecimal precoUnitario,
    Integer quantidade,
    BigDecimal pesoKg
) {
    public BigDecimal subtotal() {
        return precoUnitario.multiply(new BigDecimal(quantidade));
    }

    public BigDecimal pesoTotal() {
        return pesoKg.multiply(new BigDecimal(quantidade));
    }
}
