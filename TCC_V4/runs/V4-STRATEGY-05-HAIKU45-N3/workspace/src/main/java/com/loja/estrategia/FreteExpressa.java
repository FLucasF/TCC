package com.loja.estrategia;

import java.math.BigDecimal;

public class FreteExpressa implements EstrategiaFrete {
    @Override
    public BigDecimal calcularCusto(BigDecimal pesoTotal) {
        // R$ 25,00 + R$ 4,50 por kg
        return new BigDecimal("25.00").add(pesoTotal.multiply(new BigDecimal("4.50")));
    }

    @Override
    public int prazo() {
        return 2;
    }
}
