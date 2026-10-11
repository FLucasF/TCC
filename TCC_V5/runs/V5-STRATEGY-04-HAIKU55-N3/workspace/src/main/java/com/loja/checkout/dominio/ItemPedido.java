package com.loja.checkout.dominio;

import java.math.BigDecimal;

public record ItemPedido(BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public BigDecimal subtotal() {
        return Dinheiro.centavos(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    public BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
