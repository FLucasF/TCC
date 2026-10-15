package com.loja.checkout.entrega;

import java.math.BigDecimal;

public final class MotoboyEntrega implements ModalidadeEntrega {
    private static final BigDecimal CUSTO = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAX = new BigDecimal("5");

    @Override public BigDecimal custo(BigDecimal pesoKg) { return CUSTO; }
    @Override public int prazoDias() { return 0; }
    @Override public boolean aceita(BigDecimal pesoKg) { return pesoKg.compareTo(PESO_MAX) <= 0; }
}
