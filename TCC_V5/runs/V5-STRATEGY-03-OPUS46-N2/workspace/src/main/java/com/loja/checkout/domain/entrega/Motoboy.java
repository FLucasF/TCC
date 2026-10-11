package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return TAXA_FIXA;
    }

    @Override
    public int prazoDias() {
        return 0;
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotalKg) {
        return pesoTotalKg.compareTo(PESO_MAXIMO) <= 0;
    }
}
