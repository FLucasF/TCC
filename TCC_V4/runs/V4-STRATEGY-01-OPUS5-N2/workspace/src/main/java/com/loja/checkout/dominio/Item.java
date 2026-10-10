package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Um produto do carrinho, com preco, quantidade e peso validos. */
public record Item(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public BigDecimal total() {
        return Dinheiro.arredonda(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    public BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
