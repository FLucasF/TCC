package com.loja.domain.entrega;

import java.math.BigDecimal;

public class Motoboy implements ModalidadeEntrega {
    private static final BigDecimal LIMITE_PESO = new BigDecimal("5.00");

    @Override
    public boolean aceita(BigDecimal pesoTotal) {
        return pesoTotal.compareTo(LIMITE_PESO) <= 0;
    }

    @Override
    public ResultadoFrete calcularFrete(BigDecimal pesoTotal) {
        return new ResultadoFrete(new BigDecimal("18.00"), 0);
    }
}
