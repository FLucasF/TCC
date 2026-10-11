package com.loja.checkout.resumo;

import java.math.BigDecimal;

/** Um item do carrinho, já validado. */
public record Item(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public BigDecimal totalProdutos() {
        return Dinheiro.centavos(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    public BigDecimal pesoTotalKg() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
