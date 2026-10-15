package com.loja.resumo.dominio;

import java.math.BigDecimal;

public record Item(BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public BigDecimal valor() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
