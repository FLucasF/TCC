package br.loja.checkout;

import java.math.BigDecimal;

public record ItemCarrinho(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {
    public BigDecimal subtotal() {
        return Dinheiro.arredondar(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }
}
