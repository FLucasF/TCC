package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Um item ja validado do carrinho, usado nos calculos.
 */
public record ItemPedido(BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Preco do item vezes a quantidade (sem arredondar). */
    public BigDecimal totalProdutos() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    /** Peso do item vezes a quantidade (sem arredondar). */
    public BigDecimal pesoTotal() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
