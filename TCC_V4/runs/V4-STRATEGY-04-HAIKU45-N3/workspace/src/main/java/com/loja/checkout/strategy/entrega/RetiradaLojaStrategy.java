package com.loja.checkout.strategy.entrega;

import java.math.BigDecimal;

public class RetiradaLojaStrategy implements ModalidadeEntregaStrategy {
    @Override
    public BigDecimal calcularFrete(double pesoTotalKg) {
        return BigDecimal.ZERO;
    }

    @Override
    public int getPrazoEntregaDias() {
        return 1;
    }

    @Override
    public void validar(double pesoTotalKg) {
    }
}
