package com.loja.checkout.domain;

import java.math.BigDecimal;

/** Item do carrinho já validado (preço, quantidade e peso positivos). */
public record Item(BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Valor do item: preço × quantidade (sem arredondar). */
    public BigDecimal total() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    /** Peso do item: peso × quantidade (sem arredondar). */
    public BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
