package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Item ja validado do carrinho (preco, quantidade e peso garantidamente
 * positivos).
 */
public record Item(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Valor deste item: preco unitario x quantidade. */
    public BigDecimal valor() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    /** Peso deste item: peso unitario x quantidade. */
    public BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
