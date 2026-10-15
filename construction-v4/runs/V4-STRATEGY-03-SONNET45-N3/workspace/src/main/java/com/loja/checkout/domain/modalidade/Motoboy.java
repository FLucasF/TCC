package com.loja.checkout.domain.modalidade;

import java.math.BigDecimal;

public class Motoboy implements ModalidadeEntrega {
    private static final BigDecimal LIMITE_PESO = new BigDecimal("5.00");

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return new BigDecimal("18.00");
    }

    @Override
    public int getPrazoDias() {
        return 0;
    }

    @Override
    public boolean verificarDisponibilidade(BigDecimal pesoTotalKg) {
        return pesoTotalKg.compareTo(LIMITE_PESO) <= 0;
    }
}
