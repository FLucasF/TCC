package com.loja.model.entrega;

import com.loja.util.Dinheiro;

import java.math.BigDecimal;

public class EntregaEconomica implements ModalidadeEntrega {
    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        BigDecimal base = new BigDecimal("12.00");
        BigDecimal porKg = new BigDecimal("2.00");
        return Dinheiro.arredondar(base.add(porKg.multiply(pesoTotalKg)));
    }

    @Override
    public int getPrazoDias() {
        return 7;
    }

    @Override
    public boolean aceitaPedido(BigDecimal pesoTotalKg) {
        return true;
    }
}
