package com.loja.resumo;

import java.math.BigDecimal;

record ItemCompra(BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    BigDecimal subtotal() {
        return Dinheiro.arredondar(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
