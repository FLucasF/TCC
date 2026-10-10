package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EntregaEconomica implements OpcaoEntrega {

    private static final BigDecimal FIXO = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return FIXO.add(POR_KG.multiply(pesoTotalKg));
    }

    @Override
    public int prazoEntregaDias() {
        return 7;
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotalKg) {
        return true;
    }
}
