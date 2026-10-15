package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Um item já validado do carrinho. */
public record ItemPedido(BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public BigDecimal totalLinha() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal pesoLinha() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
