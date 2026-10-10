package com.loja.checkout.domain.modalidade;

import java.math.BigDecimal;

public class Expressa implements ModalidadeEntrega {
    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return new BigDecimal("25.00")
            .add(new BigDecimal("4.50").multiply(pesoTotalKg));
    }

    @Override
    public int getPrazoDias() {
        return 2;
    }

    @Override
    public boolean verificarDisponibilidade(BigDecimal pesoTotalKg) {
        return true;
    }
}
