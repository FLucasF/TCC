package com.loja.checkout.domain.modalidade;

import java.math.BigDecimal;

public class Economica implements ModalidadeEntrega {
    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return new BigDecimal("12.00")
            .add(new BigDecimal("2.00").multiply(pesoTotalKg));
    }

    @Override
    public int getPrazoDias() {
        return 7;
    }

    @Override
    public boolean verificarDisponibilidade(BigDecimal pesoTotalKg) {
        return true;
    }
}
