package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Um produto do carrinho, com a quantidade escolhida pelo cliente. */
public record ItemCarrinho(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Preco x quantidade, arredondado para centavos. */
    public BigDecimal totalLinha() {
        return Moeda.emCentavos(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    /** Peso x quantidade, sem arredondar. */
    public BigDecimal pesoLinha() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
