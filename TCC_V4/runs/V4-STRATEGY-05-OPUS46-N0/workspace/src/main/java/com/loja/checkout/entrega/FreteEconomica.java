package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FreteEconomica implements CalculadoraFrete {

    private static final BigDecimal BASE = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return BASE.add(POR_KG.multiply(pesoTotalKg));
    }

    @Override
    public int prazoDias() {
        return 7;
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotalKg) {
        return true;
    }
}
