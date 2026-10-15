package com.loja.dominio.modalidade;

import com.loja.dominio.Dinheiro;

import java.math.BigDecimal;

public class Expressa implements ModalidadeEntrega {
    private static final BigDecimal BASE = new BigDecimal("25.00");
    private static final BigDecimal POR_KG = new BigDecimal("4.50");

    @Override public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return Dinheiro.centavos(BASE.add(POR_KG.multiply(pesoKg)));
    }
    @Override public int prazoDias() { return 2; }
    @Override public boolean atende(BigDecimal pesoKg) { return true; }
}
