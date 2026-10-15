package com.loja.model;

import java.math.BigDecimal;

public record ItemCarrinho(
    String nome,
    BigDecimal precoUnitario,
    int quantidade,
    BigDecimal pesoKg
) {
    public boolean isValido() {
        return nome != null && !nome.isBlank()
            && precoUnitario != null && precoUnitario.compareTo(BigDecimal.ZERO) > 0
            && quantidade > 0
            && pesoKg != null && pesoKg.compareTo(BigDecimal.ZERO) > 0;
    }
}
