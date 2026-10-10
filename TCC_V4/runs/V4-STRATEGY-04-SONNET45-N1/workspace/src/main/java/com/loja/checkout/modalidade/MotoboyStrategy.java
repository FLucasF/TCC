package com.loja.checkout.modalidade;

import java.math.BigDecimal;

public class MotoboyStrategy implements ModalidadeStrategy {
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

    @Override
    public boolean estaDisponivel(BigDecimal pesoTotal) {
        return pesoTotal.compareTo(PESO_MAXIMO) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return new BigDecimal("18.00");
    }

    @Override
    public int obterPrazo() {
        return 0;
    }
}
