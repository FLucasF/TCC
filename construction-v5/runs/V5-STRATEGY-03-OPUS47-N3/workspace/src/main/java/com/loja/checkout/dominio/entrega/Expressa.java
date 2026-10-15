package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

public class Expressa implements Entrega {
    private static final BigDecimal FIXO = new BigDecimal("25.00");
    private static final BigDecimal POR_KG = new BigDecimal("4.50");

    @Override public String codigo() { return "EXPRESSA"; }

    @Override public BigDecimal frete(BigDecimal pesoKg) {
        return FIXO.add(POR_KG.multiply(pesoKg));
    }

    @Override public int prazoDias() { return 2; }

    @Override public boolean disponivelPara(BigDecimal pesoKg) { return true; }
}
