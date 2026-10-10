package com.loja.domain.entrega;

import com.loja.util.Moeda;
import java.math.BigDecimal;

public class Economica implements ModalidadeEntrega {
    @Override
    public boolean aceita(BigDecimal pesoTotal) {
        return true;
    }

    @Override
    public ResultadoFrete calcularFrete(BigDecimal pesoTotal) {
        BigDecimal base = new BigDecimal("12.00");
        BigDecimal porKg = new BigDecimal("2.00");
        BigDecimal valor = Moeda.arredondar(base.add(porKg.multiply(pesoTotal)));
        return new ResultadoFrete(valor, 7);
    }
}
