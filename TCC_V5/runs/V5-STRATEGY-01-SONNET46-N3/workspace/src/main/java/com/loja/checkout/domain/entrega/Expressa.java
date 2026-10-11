package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;
import java.math.RoundingMode;

class Expressa implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("25.00");
    private static final BigDecimal TAXA_KG = new BigDecimal("4.50");

    @Override
    public String codigo() { return "EXPRESSA"; }

    @Override
    public boolean disponivel(BigDecimal pesoKg) { return true; }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return TAXA_FIXA.add(TAXA_KG.multiply(pesoKg)).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int prazoDias() { return 2; }
}
