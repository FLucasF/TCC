package com.loja.estrategia;

import java.math.BigDecimal;

public class FreteRetiradaLoja implements EstrategiaFrete {
    @Override
    public BigDecimal calcularCusto(BigDecimal pesoTotal) {
        return BigDecimal.ZERO;
    }

    @Override
    public int prazo() {
        return 1;
    }
}
