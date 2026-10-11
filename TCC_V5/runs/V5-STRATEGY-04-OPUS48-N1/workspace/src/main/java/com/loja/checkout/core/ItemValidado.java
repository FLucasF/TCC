package com.loja.checkout.core;

import java.math.BigDecimal;

/**
 * Item do carrinho já conferido (preço, quantidade e peso presentes e positivos).
 */
public record ItemValidado(BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public BigDecimal totalItem() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal pesoTotal() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
