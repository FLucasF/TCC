package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Um item do carrinho, já validado (preço, quantidade e peso positivos).
 */
public record Item(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Valor dos produtos deste item: preço × quantidade. */
    public BigDecimal totalProdutos() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade), Money.MC);
    }

    /** Peso deste item: peso × quantidade (sem arredondar). */
    public BigDecimal pesoTotal() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade), Money.MC);
    }
}
