package com.loja.strategy.modalidade;

import java.math.BigDecimal;

public class ModalidadeRetiradaLoja implements Modalidade {
    private static final BigDecimal FRETE = new BigDecimal("0.00");
    private static final int PRAZO = 1;

    @Override
    public boolean isDisponivel(BigDecimal pesoTotal) {
        return true;
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
