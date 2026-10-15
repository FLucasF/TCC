package com.loja.checkout.domain.pedido;

import java.math.BigDecimal;

/** Item do carrinho, ja validado. */
public record ItemPedido(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Preco do item vezes a quantidade, arredondado para centavos. */
    public BigDecimal total() {
        return Dinheiro.valor(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    /** Peso do item vezes a quantidade, sem arredondar. */
    public BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
