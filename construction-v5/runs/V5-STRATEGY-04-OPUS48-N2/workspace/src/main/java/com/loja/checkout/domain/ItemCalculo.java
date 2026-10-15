package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Um item do carrinho já validado e pronto para o cálculo. Preço e peso
 * sempre positivos; quantidade sempre inteiro positivo.
 */
public record ItemCalculo(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Total da linha (preço × quantidade), arredondado para centavos. */
    public BigDecimal totalLinha() {
        return Dinheiro.centavos(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    /** Peso da linha (peso × quantidade), sem arredondar. */
    public BigDecimal pesoLinha() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
