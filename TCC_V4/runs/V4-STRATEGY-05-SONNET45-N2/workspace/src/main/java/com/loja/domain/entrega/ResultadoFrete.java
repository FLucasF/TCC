package com.loja.domain.entrega;

import java.math.BigDecimal;

public class ResultadoFrete {
    private final BigDecimal valor;
    private final int prazoDias;

    public ResultadoFrete(BigDecimal valor, int prazoDias) {
        this.valor = valor;
        this.prazoDias = prazoDias;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public int getPrazoDias() {
        return prazoDias;
    }
}
