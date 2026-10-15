package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

class RetiradaLoja implements CalculadoraFrete {
    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKgTotal) {
        return BigDecimal.ZERO;
    }

    @Override
    public int prazoEntregaDias() {
        return 1;
    }

    @Override
    public boolean disponivel(BigDecimal pesoKgTotal) {
        return true;
    }
}
