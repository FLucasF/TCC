package com.loja.model.entrega;

import com.loja.util.Dinheiro;

import java.math.BigDecimal;

public class EntregaExpressa implements ModalidadeEntrega {
    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        BigDecimal base = new BigDecimal("25.00");
        BigDecimal porKg = new BigDecimal("4.50");
        return Dinheiro.arredondar(base.add(porKg.multiply(pesoTotalKg)));
    }

    @Override
    public int getPrazoDias() {
        return 2;
    }

    @Override
    public boolean aceitaPedido(BigDecimal pesoTotalKg) {
        return true;
    }
}
