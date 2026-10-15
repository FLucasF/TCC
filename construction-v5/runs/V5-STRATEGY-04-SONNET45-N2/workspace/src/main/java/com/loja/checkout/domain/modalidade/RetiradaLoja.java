package com.loja.checkout.domain.modalidade;

import java.math.BigDecimal;

public class RetiradaLoja implements ModalidadeEntrega {
    private static final int PRAZO_DIAS = 1;

    @Override
    public boolean estaDisponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularValor(BigDecimal pesoTotalKg) {
        return new BigDecimal("0.00");
    }

    @Override
    public int obterPrazoDias() {
        return PRAZO_DIAS;
    }
}
