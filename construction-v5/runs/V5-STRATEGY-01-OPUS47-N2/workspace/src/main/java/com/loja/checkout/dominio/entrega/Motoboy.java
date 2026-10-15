package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

public final class Motoboy implements Modalidade {
    private static final BigDecimal LIMITE_KG = new BigDecimal("5");
    private static final BigDecimal CUSTO = new BigDecimal("18.00");

    public boolean atende(BigDecimal pesoKg) {
        return pesoKg.compareTo(LIMITE_KG) <= 0;
    }

    public BigDecimal custo(BigDecimal pesoKg) { return CUSTO; }

    public int prazoDias() { return 0; }
}
