package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;
import java.math.RoundingMode;

class Economica implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("12.00");
    private static final BigDecimal TAXA_KG = new BigDecimal("2.00");

    @Override
    public String codigo() { return "ECONOMICA"; }

    @Override
    public boolean disponivel(BigDecimal pesoKg) { return true; }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return TAXA_FIXA.add(TAXA_KG.multiply(pesoKg)).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int prazoDias() { return 7; }
}
