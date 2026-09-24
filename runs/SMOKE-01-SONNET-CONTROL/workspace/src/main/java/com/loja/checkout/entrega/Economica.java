package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component("ECONOMICA")
public class Economica implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("12.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("2.00");

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return TAXA_FIXA.add(TAXA_POR_KG.multiply(pesoTotalKg));
    }

    @Override
    public int prazoEntregaDias() {
        return 7;
    }
}
