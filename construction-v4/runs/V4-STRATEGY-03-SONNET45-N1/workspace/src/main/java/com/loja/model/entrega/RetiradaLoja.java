package com.loja.model.entrega;

import java.math.BigDecimal;

public class RetiradaLoja implements ModalidadeEntrega {
    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return new BigDecimal("0.00");
    }

    @Override
    public int getPrazoDias() {
        return 1;
    }

    @Override
    public boolean aceitaPedido(BigDecimal pesoTotalKg) {
        return true;
    }
}
