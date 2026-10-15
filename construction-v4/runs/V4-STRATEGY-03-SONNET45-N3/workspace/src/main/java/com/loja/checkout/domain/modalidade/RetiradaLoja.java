package com.loja.checkout.domain.modalidade;

import java.math.BigDecimal;

public class RetiradaLoja implements ModalidadeEntrega {
    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return BigDecimal.ZERO;
    }

    @Override
    public int getPrazoDias() {
        return 1;
    }

    @Override
    public boolean verificarDisponibilidade(BigDecimal pesoTotalKg) {
        return true;
    }
}
