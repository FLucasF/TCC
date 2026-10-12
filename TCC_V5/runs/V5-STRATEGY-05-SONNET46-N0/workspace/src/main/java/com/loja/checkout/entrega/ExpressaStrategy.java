package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ExpressaStrategy implements ModalidadeEntregaStrategy {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("25.00");
    private static final BigDecimal TAXA_KG = new BigDecimal("4.50");

    @Override
    public String getCodigo() {
        return "EXPRESSA";
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return TAXA_FIXA.add(TAXA_KG.multiply(pesoTotalKg));
    }

    @Override
    public int getPrazoDias() {
        return 2;
    }

    @Override
    public boolean atende(BigDecimal pesoTotalKg) {
        return true;
    }
}
