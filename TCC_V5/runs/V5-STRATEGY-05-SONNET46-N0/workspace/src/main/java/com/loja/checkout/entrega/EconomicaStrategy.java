package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EconomicaStrategy implements ModalidadeEntregaStrategy {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("12.00");
    private static final BigDecimal TAXA_KG = new BigDecimal("2.00");

    @Override
    public String getCodigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return TAXA_FIXA.add(TAXA_KG.multiply(pesoTotalKg));
    }

    @Override
    public int getPrazoDias() {
        return 7;
    }

    @Override
    public boolean atende(BigDecimal pesoTotalKg) {
        return true;
    }
}
