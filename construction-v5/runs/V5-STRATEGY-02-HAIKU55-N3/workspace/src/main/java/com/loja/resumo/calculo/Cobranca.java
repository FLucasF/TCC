package com.loja.resumo.calculo;

import java.math.BigDecimal;

record Cobranca(BigDecimal totalFinal, BigDecimal valorParcela) {

    static Cobranca avista(BigDecimal valor) {
        return new Cobranca(valor, valor);
    }
}
