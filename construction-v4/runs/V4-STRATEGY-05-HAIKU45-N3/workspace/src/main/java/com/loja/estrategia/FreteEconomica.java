package com.loja.estrategia;

import java.math.BigDecimal;

public class FreteEconomica implements EstrategiaFrete {
    @Override
    public BigDecimal calcularCusto(BigDecimal pesoTotal) {
        // R$ 12,00 + R$ 2,00 por kg
        return new BigDecimal("12.00").add(pesoTotal.multiply(new BigDecimal("2.00")));
    }

    @Override
    public int prazo() {
        return 7;
    }
}
