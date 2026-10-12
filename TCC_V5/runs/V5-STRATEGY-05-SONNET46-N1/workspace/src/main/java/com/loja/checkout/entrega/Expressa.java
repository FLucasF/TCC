package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Expressa implements ModalidadeEntrega {
    private static final BigDecimal BASE = new BigDecimal("25.00");
    private static final BigDecimal POR_KG = new BigDecimal("4.50");

    @Override
    public String codigo() { return "EXPRESSA"; }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return BASE.add(POR_KG.multiply(pesoTotalKg));
    }

    @Override
    public int prazoEntregaDias() { return 2; }

    @Override
    public void validarDisponibilidade(BigDecimal pesoTotalKg) {}
}
