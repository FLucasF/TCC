package com.loja.checkout.strategy;

import java.math.BigDecimal;

public class ModalidadeMotoboy implements ModalidadeEntrega {
    private static final double PESO_MAXIMO_KG = 5.0;

    @Override public String codigo() { return "MOTOBOY"; }
    @Override public int prazoEntregaDias() { return 0; }
    @Override public boolean aceita(double pesoKgTotal) { return pesoKgTotal <= PESO_MAXIMO_KG; }
    @Override public BigDecimal calcularFrete(double pesoKgTotal) { return BigDecimal.valueOf(18.00).setScale(2); }
}
