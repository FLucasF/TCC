package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

public interface CalculadoraFrete {
    BigDecimal calcularFrete(BigDecimal pesoKgTotal);
    int prazoEntregaDias();
    boolean disponivel(BigDecimal pesoKgTotal);
}
