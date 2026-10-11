package com.loja.checkout.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ModalidadeEconomica implements ModalidadeEntrega {
    @Override public String codigo() { return "ECONOMICA"; }
    @Override public int prazoEntregaDias() { return 7; }
    @Override public boolean aceita(double pesoKgTotal) { return true; }

    @Override
    public BigDecimal calcularFrete(double pesoKgTotal) {
        return BigDecimal.valueOf(12.00)
                .add(BigDecimal.valueOf(2.00).multiply(BigDecimal.valueOf(pesoKgTotal)))
                .setScale(2, RoundingMode.HALF_EVEN);
    }
}
