package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Um item do carrinho ja validado. Sabe calcular o proprio valor (preco x
 * quantidade) e o proprio peso (peso x quantidade).
 */
public record ItemPedido(BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public BigDecimal valor() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
