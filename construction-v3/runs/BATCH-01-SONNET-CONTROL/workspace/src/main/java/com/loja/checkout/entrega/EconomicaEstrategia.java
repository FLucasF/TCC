package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EconomicaEstrategia implements ModalidadeEntregaEstrategia {

    private static final BigDecimal TAXA_BASE = new BigDecimal("12.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("2.00");

    @Override
    public String getCodigo() {
        return "ECONOMICA";
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return TAXA_BASE.add(TAXA_POR_KG.multiply(pesoTotalKg));
    }

    @Override
    public int prazoEntregaDias() {
        return 7;
    }
}
