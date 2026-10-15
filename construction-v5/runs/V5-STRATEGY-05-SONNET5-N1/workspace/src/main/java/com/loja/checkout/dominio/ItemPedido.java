package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Item de pedido já validado (preço, quantidade e peso presentes e positivos).
 */
public record ItemPedido(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal pesoTotal() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
