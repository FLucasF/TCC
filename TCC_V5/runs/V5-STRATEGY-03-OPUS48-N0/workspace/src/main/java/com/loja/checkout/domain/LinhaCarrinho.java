package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Representacao interna de um item ja validado do carrinho, usada pelos calculos.
 */
public record LinhaCarrinho(BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Preco da linha: preco unitario x quantidade (sem arredondar). */
    public BigDecimal totalLinha() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    /** Peso da linha: peso unitario x quantidade (sem arredondar). */
    public BigDecimal pesoLinha() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
