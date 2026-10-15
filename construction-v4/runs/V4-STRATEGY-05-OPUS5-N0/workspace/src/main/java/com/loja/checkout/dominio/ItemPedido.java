package com.loja.checkout.dominio;

import com.loja.checkout.comum.Dinheiro;

import java.math.BigDecimal;

/** Um produto do carrinho, com a quantidade pedida. */
public record ItemPedido(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Preco x quantidade, arredondado para centavos. */
    public BigDecimal total() {
        return Dinheiro.valor(precoUnitario.multiply(BigDecimal.valueOf(quantidade), Dinheiro.CALCULO));
    }

    /** Peso x quantidade, sem arredondar. */
    public BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade), Dinheiro.CALCULO);
    }
}
