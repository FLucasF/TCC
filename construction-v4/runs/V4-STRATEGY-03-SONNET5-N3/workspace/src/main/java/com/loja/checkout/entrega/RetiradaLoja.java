package com.loja.checkout.entrega;

import java.math.BigDecimal;

public class RetiradaLoja implements OpcaoEntrega {

    @Override
    public BigDecimal custo(BigDecimal pesoKg) {
        return BigDecimal.ZERO;
    }

    @Override
    public int prazoDias() {
        return 1;
    }

    @Override
    public boolean disponivel(BigDecimal pesoKg) {
        return true;
    }
}
