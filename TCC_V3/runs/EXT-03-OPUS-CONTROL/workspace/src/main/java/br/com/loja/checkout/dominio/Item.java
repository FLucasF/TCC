package br.com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Um item do carrinho, ja validado. */
public record Item(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Preco x quantidade, arredondado em centavos. */
    public BigDecimal total() {
        return Dinheiro.centavos(precoUnitario.multiply(BigDecimal.valueOf(quantidade), Dinheiro.PRECISAO));
    }

    /** Peso x quantidade, sem arredondamento. */
    public BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade), Dinheiro.PRECISAO);
    }
}
