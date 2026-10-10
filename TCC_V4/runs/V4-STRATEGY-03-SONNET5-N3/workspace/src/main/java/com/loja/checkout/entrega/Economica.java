package com.loja.checkout.entrega;

import java.math.BigDecimal;

public class Economica implements OpcaoEntrega {

    private static final BigDecimal BASE = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public BigDecimal custo(BigDecimal pesoKg) {
        return BASE.add(POR_KG.multiply(pesoKg));
    }

    @Override
    public int prazoDias() {
        return 7;
    }

    @Override
    public boolean disponivel(BigDecimal pesoKg) {
        return true;
    }
}
