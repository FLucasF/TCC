package com.loja.checkout.calculo;

import java.math.BigDecimal;

import static com.loja.checkout.calculo.Dinheiro.centavos;

record ItemCarrinho(BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    BigDecimal total() {
        return centavos(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    BigDecimal pesoTotal() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
