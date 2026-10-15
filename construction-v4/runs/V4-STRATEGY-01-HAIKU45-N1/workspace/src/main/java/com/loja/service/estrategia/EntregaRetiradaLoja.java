package com.loja.service.estrategia;

import java.math.BigDecimal;

public class EntregaRetiradaLoja implements CalculoEntrega {
    private static final Integer PRAZO = 1;

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return BigDecimal.ZERO;
    }

    @Override
    public Integer getPrazo() {
        return PRAZO;
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotal) {
        return true;
    }
}
