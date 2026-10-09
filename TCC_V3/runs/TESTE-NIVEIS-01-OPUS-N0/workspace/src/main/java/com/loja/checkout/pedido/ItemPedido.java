package com.loja.checkout.pedido;

import java.math.BigDecimal;

/** Um produto do carrinho, ja validado. */
public record ItemPedido(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Preco unitario x quantidade, sem arredondar. */
    public BigDecimal valorTotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    /** Peso unitario x quantidade, sem arredondar. */
    public BigDecimal pesoTotalKg() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
