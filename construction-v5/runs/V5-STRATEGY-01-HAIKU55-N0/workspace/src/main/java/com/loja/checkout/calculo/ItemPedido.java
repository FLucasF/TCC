package com.loja.checkout.calculo;

import java.math.BigDecimal;

import static com.loja.checkout.calculo.Dinheiro.centavos;

record ItemPedido(BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    BigDecimal subtotal() {
        return centavos(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
