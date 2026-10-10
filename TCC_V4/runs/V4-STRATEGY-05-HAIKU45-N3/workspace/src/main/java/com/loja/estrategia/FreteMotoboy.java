package com.loja.estrategia;

import java.math.BigDecimal;

public class FreteMotoboy implements EstrategiaFrete {
    @Override
    public BigDecimal calcularCusto(BigDecimal pesoTotal) {
        return new BigDecimal("18.00");
    }

    @Override
    public int prazo() {
        return 0;
    }
}
