package com.loja.dominio.modalidade;

import com.loja.dominio.Dinheiro;

import java.math.BigDecimal;

public class Economica implements ModalidadeEntrega {
    private static final BigDecimal BASE = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return Dinheiro.centavos(BASE.add(POR_KG.multiply(pesoKg)));
    }
    @Override public int prazoDias() { return 7; }
    @Override public boolean atende(BigDecimal pesoKg) { return true; }
}
