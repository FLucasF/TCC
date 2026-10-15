package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Um produto do carrinho, com a quantidade pedida. */
public record Item(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Valor dos produtos desta linha do carrinho. */
    public BigDecimal total() {
        return Dinheiro.centavos(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    /** Peso desta linha do carrinho, sem arredondar. */
    public BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
