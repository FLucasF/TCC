package com.loja.checkout.entrega;

import java.math.BigDecimal;

public class Expressa implements OpcaoEntrega {

    private static final BigDecimal BASE = new BigDecimal("25.00");
    private static final BigDecimal POR_KG = new BigDecimal("4.50");

    @Override
    public BigDecimal custo(BigDecimal pesoKg) {
        return BASE.add(POR_KG.multiply(pesoKg));
    }

    @Override
    public int prazoDias() {
        return 2;
    }

    @Override
    public boolean disponivel(BigDecimal pesoKg) {
        return true;
    }
}
