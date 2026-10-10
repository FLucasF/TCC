package com.loja.checkout.dominio;

import java.math.BigDecimal;

public record ItemPedido(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Preco do item vezes a quantidade, arredondado para centavos. */
    public BigDecimal total() {
        return Centavos.arredondar(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    /** Peso do item vezes a quantidade, sem arredondar. */
    public BigDecimal pesoTotalKg() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
