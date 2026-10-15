package com.loja.dominio.modalidade;

import com.loja.dominio.Dinheiro;

import java.math.BigDecimal;

public class Motoboy implements ModalidadeEntrega {
    private static final BigDecimal CUSTO = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAX = new BigDecimal("5");

    @Override public BigDecimal calcularFrete(BigDecimal pesoKg) { return Dinheiro.centavos(CUSTO); }
    @Override public int prazoDias() { return 0; }
    @Override public boolean atende(BigDecimal pesoKg) { return pesoKg.compareTo(PESO_MAX) <= 0; }
}
