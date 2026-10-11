package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Um item do carrinho, ja validado. */
public record ItemPedido(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Preco unitario x quantidade, arredondado para centavos. */
    public BigDecimal total() {
        return Dinheiro.centavos(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    /** Peso unitario x quantidade, sem arredondar. */
    public BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
