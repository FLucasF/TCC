package com.loja.checkout.strategy.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntregaStrategy {
    BigDecimal calcularFrete(double pesoTotalKg);

    int getPrazoEntregaDias();

    void validar(double pesoTotalKg);
}
