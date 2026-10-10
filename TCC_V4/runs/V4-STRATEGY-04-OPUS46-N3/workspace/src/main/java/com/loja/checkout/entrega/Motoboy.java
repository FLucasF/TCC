package com.loja.checkout.entrega;

import java.math.BigDecimal;

public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal FRETE = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return FRETE;
    }

    @Override
    public int prazoDias() {
        return 0;
    }

    @Override
    public boolean disponivel(BigDecimal pesoKg) {
        return pesoKg.compareTo(PESO_MAXIMO) <= 0;
    }
}
