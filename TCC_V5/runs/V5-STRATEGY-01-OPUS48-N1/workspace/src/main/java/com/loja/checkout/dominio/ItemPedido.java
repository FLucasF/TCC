package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Item já validado do carrinho. Separado do DTO de entrada para o domínio não
 * depender do formato que chega pela web.
 */
public record ItemPedido(BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public BigDecimal totalItem() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal pesoItem() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
