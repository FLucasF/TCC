package com.loja.checkout.entrega;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class Economica implements ModalidadeEntrega {

    private static final BigDecimal BASE = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public String getCodigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return BASE.add(POR_KG.multiply(pesoTotalKg)).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int getPrazoDias() {
        return 7;
    }

    @Override
    public boolean isDisponivel(BigDecimal pesoTotalKg) {
        return true;
    }
}
