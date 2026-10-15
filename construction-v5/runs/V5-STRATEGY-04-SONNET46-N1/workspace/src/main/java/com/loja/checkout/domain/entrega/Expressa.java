package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

class Expressa implements CalculadoraFrete {
    private static final BigDecimal BASE = new BigDecimal("25.00");
    private static final BigDecimal POR_KG = new BigDecimal("4.50");

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKgTotal) {
        return BASE.add(POR_KG.multiply(pesoKgTotal));
    }

    @Override
    public int prazoEntregaDias() {
        return 2;
    }

    @Override
    public boolean disponivel(BigDecimal pesoKgTotal) {
        return true;
    }
}
