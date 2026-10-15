package com.loja.strategy.entrega;

import java.math.BigDecimal;

public class Motoboy implements ModalidadeEntrega {
    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return new BigDecimal("18.00");
    }

    @Override
    public int getPrazo() {
        return 0;
    }

    @Override
    public boolean aceita(BigDecimal pesoTotal) {
        return pesoTotal.compareTo(new BigDecimal("5.0")) <= 0;
    }
}
