package com.loja.strategy.modalidade;

import java.math.BigDecimal;

public class ModalidadeMotoboy implements Modalidade {
    private static final BigDecimal FRETE = new BigDecimal("18.00");
    private static final int PRAZO = 0;
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5.00");

    @Override
    public boolean isDisponivel(BigDecimal pesoTotal) {
        return pesoTotal.compareTo(PESO_MAXIMO) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return FRETE;
    }

    @Override
    public int getPrazoEntregaDias() {
        return PRAZO;
    }
}
