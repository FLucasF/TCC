package com.loja.checkout.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ModalidadeExpressa implements ModalidadeEntrega {
    @Override public String codigo() { return "EXPRESSA"; }
    @Override public int prazoEntregaDias() { return 2; }
    @Override public boolean aceita(double pesoKgTotal) { return true; }

    @Override
    public BigDecimal calcularFrete(double pesoKgTotal) {
        return BigDecimal.valueOf(25.00)
                .add(BigDecimal.valueOf(4.50).multiply(BigDecimal.valueOf(pesoKgTotal)))
                .setScale(2, RoundingMode.HALF_EVEN);
    }
}
