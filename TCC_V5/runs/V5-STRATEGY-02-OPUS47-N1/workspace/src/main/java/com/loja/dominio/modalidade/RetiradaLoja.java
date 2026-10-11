package com.loja.dominio.modalidade;

import com.loja.dominio.Dinheiro;

import java.math.BigDecimal;

public class RetiradaLoja implements ModalidadeEntrega {
    @Override public BigDecimal calcularFrete(BigDecimal pesoKg) { return Dinheiro.ZERO; }
    @Override public int prazoDias() { return 1; }
    @Override public boolean atende(BigDecimal pesoKg) { return true; }
}
