package com.loja.checkout.modalidade;

import com.loja.checkout.util.Arredondamento;

import java.math.BigDecimal;

public class EconomicaStrategy implements ModalidadeStrategy {
    @Override
    public boolean estaDisponivel(BigDecimal pesoTotal) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        BigDecimal base = new BigDecimal("12.00");
        BigDecimal porKg = new BigDecimal("2.00");
        return Arredondamento.arredondar(base.add(porKg.multiply(pesoTotal)));
    }

    @Override
    public int obterPrazo() {
        return 7;
    }
}
