package com.loja.checkout.estrategia.entrega;

import java.math.BigDecimal;

public class EntregaRetiradaLoja implements EstrategiaEntrega {
    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return BigDecimal.ZERO;
    }

    @Override
    public int getPrazo() {
        return 1;
    }

    @Override
    public void validar(BigDecimal pesoTotal) throws IllegalArgumentException {
    }
}
