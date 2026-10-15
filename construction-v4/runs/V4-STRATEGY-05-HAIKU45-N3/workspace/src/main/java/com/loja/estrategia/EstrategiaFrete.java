package com.loja.estrategia;

import java.math.BigDecimal;

public interface EstrategiaFrete {
    BigDecimal calcularCusto(BigDecimal pesoTotal);
    int prazo();
}
