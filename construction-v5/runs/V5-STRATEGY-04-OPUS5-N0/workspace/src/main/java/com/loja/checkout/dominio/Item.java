package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Um item do carrinho, já validado. */
public record Item(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Preço × quantidade, arredondado para centavos. */
    public BigDecimal totalLinha() {
        return Dinheiro.arredondar(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    /** Peso × quantidade, sem arredondar. */
    public BigDecimal pesoTotalKg() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
