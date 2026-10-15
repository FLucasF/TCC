package com.loja.checkout.entrega;

import java.math.BigDecimal;

public final class RetiradaLojaEntrega implements ModalidadeEntrega {
    @Override public BigDecimal custo(BigDecimal pesoKg) { return BigDecimal.ZERO; }
    @Override public int prazoDias() { return 1; }
    @Override public boolean aceita(BigDecimal pesoKg) { return true; }
}
