package com.loja.strategy.entrega;

import java.math.BigDecimal;

public class RetiradaLoja implements ModalidadeEntrega {
    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int getPrazo() {
        return 1;
    }

    @Override
    public boolean aceita(BigDecimal pesoTotal) {
        return true;
    }
}
