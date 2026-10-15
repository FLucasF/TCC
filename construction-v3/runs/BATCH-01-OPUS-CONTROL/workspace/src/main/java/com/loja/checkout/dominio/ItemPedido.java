package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Uma linha do carrinho. */
public record ItemPedido(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Total da linha (preco x quantidade), arredondado para centavos. */
    public BigDecimal total() {
        return Dinheiro.centavos(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    /** Peso da linha (peso x quantidade), sem arredondar. */
    public BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }

    /** Quantidade de unidades gratuitas na promocao "leve N pague N-1". */
    public int unidadesGratis(int aCada) {
        return quantidade / aCada;
    }
}
