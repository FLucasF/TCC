package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class EntregaExpressa implements CalculoEntrega {

    private static final BigDecimal FIXO = new BigDecimal("25.00");
    private static final BigDecimal POR_KG = new BigDecimal("4.50");

    @Override
    public String getCodigo() {
        return "EXPRESSA";
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return FIXO.add(POR_KG.multiply(pesoTotalKg)).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int getPrazoDias() {
        return 2;
    }

    @Override
    public boolean isDisponivel(BigDecimal pesoTotalKg) {
        return true;
    }
}
