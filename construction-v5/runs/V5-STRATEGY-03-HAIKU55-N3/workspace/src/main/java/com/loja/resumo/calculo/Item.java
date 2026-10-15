package com.loja.resumo.calculo;

import java.math.BigDecimal;

import static com.loja.resumo.calculo.Dinheiro.arredonda;

record Item(BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {

    Item {
        if (precoUnitario == null || precoUnitario.signum() <= 0
                || quantidade == null || quantidade <= 0
                || pesoKg == null || pesoKg.signum() <= 0) {
            throw new ResumoException("PEDIDO_INVALIDO");
        }
    }

    BigDecimal subtotal() {
        return arredonda(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
