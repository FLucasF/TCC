package br.com.loja.checkout.carrinho;

import br.com.loja.checkout.Dinheiro;

import java.math.BigDecimal;

public record Item(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public BigDecimal valorTotal() {
        return Dinheiro.centavos(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    public BigDecimal pesoTotal() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
