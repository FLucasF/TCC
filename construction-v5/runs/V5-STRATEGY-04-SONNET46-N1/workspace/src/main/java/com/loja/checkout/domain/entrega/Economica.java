package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

class Economica implements CalculadoraFrete {
    private static final BigDecimal BASE = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKgTotal) {
        return BASE.add(POR_KG.multiply(pesoKgTotal));
    }

    @Override
    public int prazoEntregaDias() {
        return 7;
    }

    @Override
    public boolean disponivel(BigDecimal pesoKgTotal) {
        return true;
    }
}
