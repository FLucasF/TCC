package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Um item do carrinho, ja validado (preco, quantidade e peso positivos).
 */
public record ItemPedido(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Valor da linha: preco unitario x quantidade. */
    public BigDecimal totalLinha() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    /** Peso da linha: peso unitario x quantidade (sem arredondar). */
    public BigDecimal pesoLinha() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
