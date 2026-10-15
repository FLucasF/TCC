package com.loja.service.estrategia;

import java.math.BigDecimal;

public class EntregaMotoboy implements CalculoEntrega {
    private static final BigDecimal TAXA_FIXA = new BigDecimal("18.00");
    private static final Integer PRAZO = 0;
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5.00");

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return TAXA_FIXA;
    }

    @Override
    public Integer getPrazo() {
        return PRAZO;
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotal) {
        return pesoTotal.compareTo(PESO_MAXIMO) <= 0;
    }
}
