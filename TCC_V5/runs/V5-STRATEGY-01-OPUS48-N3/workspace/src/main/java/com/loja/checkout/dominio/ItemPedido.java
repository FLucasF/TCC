package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Um item já validado do carrinho. Preço e peso nunca nulos ou não positivos.
 */
public record ItemPedido(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public BigDecimal totalItem() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal pesoItem() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
