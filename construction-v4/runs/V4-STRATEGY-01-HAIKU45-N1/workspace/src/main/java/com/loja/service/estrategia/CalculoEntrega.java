package com.loja.service.estrategia;

import java.math.BigDecimal;

public interface CalculoEntrega {
    BigDecimal calcularFrete(BigDecimal pesoTotal);
    Integer getPrazo();
    boolean disponivel(BigDecimal pesoTotal);
}
