package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

class Motoboy implements CalculadoraFrete {
    private static final BigDecimal FRETE = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKgTotal) {
        return FRETE;
    }

    @Override
    public int prazoEntregaDias() {
        return 0;
    }

    @Override
    public boolean disponivel(BigDecimal pesoKgTotal) {
        return pesoKgTotal.compareTo(PESO_MAXIMO) <= 0;
    }
}
