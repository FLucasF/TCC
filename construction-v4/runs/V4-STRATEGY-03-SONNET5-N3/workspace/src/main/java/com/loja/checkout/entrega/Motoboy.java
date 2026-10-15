package com.loja.checkout.entrega;

import java.math.BigDecimal;

public class Motoboy implements OpcaoEntrega {

    private static final BigDecimal CUSTO = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public BigDecimal custo(BigDecimal pesoKg) {
        return CUSTO;
    }

    @Override
    public int prazoDias() {
        return 0;
    }

    @Override
    public boolean disponivel(BigDecimal pesoKg) {
        return pesoKg.compareTo(PESO_MAXIMO_KG) <= 0;
    }
}
