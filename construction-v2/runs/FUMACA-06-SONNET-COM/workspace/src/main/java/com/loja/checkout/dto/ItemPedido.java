package com.loja.checkout.dto;

import java.math.BigDecimal;

public record ItemPedido(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {

    public boolean valido() {
        return precoUnitario != null && precoUnitario.signum() > 0
                && quantidade != null && quantidade > 0
                && pesoKg != null && pesoKg.signum() > 0;
    }

    public BigDecimal totalPreco() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal totalPeso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
