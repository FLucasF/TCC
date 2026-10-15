package com.loja.checkout.domain;

import java.math.BigDecimal;

/** Um produto do carrinho, já validado. */
public record Item(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Valor do item: preço unitário x quantidade. */
    public BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    /** Peso do item: peso unitário x quantidade (sem arredondar). */
    public BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }

    /** Unidades que saem de graça a cada 3 iguais (regra do LEVE3PAGUE2). */
    public int unidadesGratis() {
        return quantidade / 3;
    }
}
