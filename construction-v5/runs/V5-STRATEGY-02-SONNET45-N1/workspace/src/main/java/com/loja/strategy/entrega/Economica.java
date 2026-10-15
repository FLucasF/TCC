package com.loja.strategy.entrega;

import com.loja.util.Arredondamento;
import java.math.BigDecimal;

public class Economica implements ModalidadeEntrega {
    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        BigDecimal base = new BigDecimal("12.00");
        BigDecimal porKg = new BigDecimal("2.00");
        return Arredondamento.arredondar(base.add(porKg.multiply(pesoTotal)));
    }

    @Override
    public int getPrazo() {
        return 7;
    }

    @Override
    public boolean aceita(BigDecimal pesoTotal) {
        return true;
    }
}
