package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

public class Motoboy implements Entrega {
    private static final BigDecimal FIXO = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAX = new BigDecimal("5");

    @Override public String codigo() { return "MOTOBOY"; }
    @Override public BigDecimal frete(BigDecimal pesoKg) { return FIXO; }
    @Override public int prazoDias() { return 0; }
    @Override public boolean disponivelPara(BigDecimal pesoKg) {
        return pesoKg.compareTo(PESO_MAX) <= 0;
    }
}
