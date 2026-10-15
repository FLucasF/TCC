package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Um produto do carrinho, já validado — por isso os valores aqui nunca são
 * nulos nem zerados, ao contrário do que chega do site.
 */
public record Item(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Valor da linha do item, já em centavos. */
    public BigDecimal total() {
        return valorDe(quantidade);
    }

    /** Valor de uma parte das unidades do item, já em centavos. */
    public BigDecimal valorDe(int unidades) {
        return Dinheiro.centavos(precoUnitario.multiply(BigDecimal.valueOf(unidades)));
    }

    /** Peso do item no pedido, sem arredondar. */
    public BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
