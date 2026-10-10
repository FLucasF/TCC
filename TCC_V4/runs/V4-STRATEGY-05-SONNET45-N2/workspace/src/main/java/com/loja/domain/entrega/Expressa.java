package com.loja.domain.entrega;

import com.loja.util.Moeda;
import java.math.BigDecimal;

public class Expressa implements ModalidadeEntrega {
    @Override
    public boolean aceita(BigDecimal pesoTotal) {
        return true;
    }

    @Override
    public ResultadoFrete calcularFrete(BigDecimal pesoTotal) {
        BigDecimal base = new BigDecimal("25.00");
        BigDecimal porKg = new BigDecimal("4.50");
        BigDecimal valor = Moeda.arredondar(base.add(porKg.multiply(pesoTotal)));
        return new ResultadoFrete(valor, 2);
    }
}
