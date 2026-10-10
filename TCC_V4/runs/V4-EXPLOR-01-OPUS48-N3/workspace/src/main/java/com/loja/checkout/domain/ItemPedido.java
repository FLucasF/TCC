package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Item já validado do carrinho. Preço e peso por unidade; quantidade inteira.
 */
public record ItemPedido(BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public BigDecimal totalPreco() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal totalPeso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
