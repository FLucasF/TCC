package com.loja.checkout.strategy;

import java.math.BigDecimal;

public interface EntregaStrategy {
    BigDecimal calcularFrete(Double pesoTotal);
    Integer getPrazo();
    boolean isDisponivel(Double pesoTotal);
}
