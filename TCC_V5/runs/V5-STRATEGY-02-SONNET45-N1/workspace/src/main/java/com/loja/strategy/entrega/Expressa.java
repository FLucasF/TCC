package com.loja.strategy.entrega;

import com.loja.util.Arredondamento;
import java.math.BigDecimal;

public class Expressa implements ModalidadeEntrega {
    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        BigDecimal base = new BigDecimal("25.00");
        BigDecimal porKg = new BigDecimal("4.50");
        return Arredondamento.arredondar(base.add(porKg.multiply(pesoTotal)));
    }

    @Override
    public int getPrazo() {
        return 2;
    }

    @Override
    public boolean aceita(BigDecimal pesoTotal) {
        return true;
    }
}
