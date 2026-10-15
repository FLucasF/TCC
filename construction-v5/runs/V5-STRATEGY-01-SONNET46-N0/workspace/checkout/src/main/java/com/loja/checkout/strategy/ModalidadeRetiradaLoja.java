package com.loja.checkout.strategy;

import java.math.BigDecimal;

public class ModalidadeRetiradaLoja implements ModalidadeEntrega {
    @Override public String codigo() { return "RETIRADA_LOJA"; }
    @Override public int prazoEntregaDias() { return 1; }
    @Override public boolean aceita(double pesoKgTotal) { return true; }
    @Override public BigDecimal calcularFrete(double pesoKgTotal) { return BigDecimal.ZERO.setScale(2); }
}
