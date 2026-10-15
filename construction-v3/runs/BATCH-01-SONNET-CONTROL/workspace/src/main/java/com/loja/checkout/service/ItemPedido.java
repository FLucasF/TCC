package com.loja.checkout.service;

import java.math.BigDecimal;

public record ItemPedido(
        String nome,
        BigDecimal precoUnitario,
        int quantidade,
        BigDecimal pesoKg
) {

    public BigDecimal totalItem() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal pesoTotalItem() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
