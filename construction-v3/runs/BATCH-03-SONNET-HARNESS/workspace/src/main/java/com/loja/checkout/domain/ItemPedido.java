package com.loja.checkout.domain;

import java.math.BigDecimal;

public record ItemPedido(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public boolean valido() {
        return precoUnitario != null && precoUnitario.signum() > 0
                && quantidade > 0
                && pesoKg != null && pesoKg.signum() > 0;
    }

    public BigDecimal totalItem() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal pesoTotalItem() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
