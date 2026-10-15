package com.loja.checkout.entrega;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Economica implements ModalidadeEntrega {

    private static final BigDecimal FIXO = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return FIXO.add(POR_KG.multiply(pesoKg)).setScale(2, RoundingMode.HALF_EVEN);
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
