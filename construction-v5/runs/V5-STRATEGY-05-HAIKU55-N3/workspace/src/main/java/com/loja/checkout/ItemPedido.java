package com.loja.checkout;

import java.math.BigDecimal;

record ItemPedido(BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    BigDecimal subtotal() {
        return Dinheiro.centavos(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
