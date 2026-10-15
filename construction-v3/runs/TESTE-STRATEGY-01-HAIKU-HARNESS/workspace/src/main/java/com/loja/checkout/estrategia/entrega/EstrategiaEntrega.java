package com.loja.checkout.estrategia.entrega;

import java.math.BigDecimal;

public interface EstrategiaEntrega {
    BigDecimal calcularFrete(BigDecimal pesoTotal);
    int getPrazo();
    void validar(BigDecimal pesoTotal) throws IllegalArgumentException;
}
