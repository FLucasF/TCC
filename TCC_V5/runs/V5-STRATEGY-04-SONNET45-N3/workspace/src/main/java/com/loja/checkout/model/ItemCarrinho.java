package com.loja.checkout.model;

import java.math.BigDecimal;

public record ItemCarrinho(
    String nome,
    BigDecimal precoUnitario,
    int quantidade,
    BigDecimal pesoKg
) {
    public BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal pesoTotal() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }

    public boolean valido() {
        return nome != null && !nome.isBlank()
            && precoUnitario != null && precoUnitario.compareTo(BigDecimal.ZERO) > 0
            && quantidade > 0
            && pesoKg != null && pesoKg.compareTo(BigDecimal.ZERO) > 0;
    }
}
