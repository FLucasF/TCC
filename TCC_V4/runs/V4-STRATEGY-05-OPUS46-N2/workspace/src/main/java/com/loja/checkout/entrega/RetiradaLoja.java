package com.loja.checkout.entrega;

import java.math.BigDecimal;

public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return BigDecimal.ZERO.setScale(2);
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
