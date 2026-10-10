package com.loja.checkout.modalidade;

import com.loja.checkout.util.Arredondamento;

import java.math.BigDecimal;

public class ExpressaStrategy implements ModalidadeStrategy {
    @Override
    public boolean estaDisponivel(BigDecimal pesoTotal) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        BigDecimal base = new BigDecimal("25.00");
        BigDecimal porKg = new BigDecimal("4.50");
        return Arredondamento.arredondar(base.add(porKg.multiply(pesoTotal)));
    }

    @Override
    public int obterPrazo() {
        return 2;
    }
}
