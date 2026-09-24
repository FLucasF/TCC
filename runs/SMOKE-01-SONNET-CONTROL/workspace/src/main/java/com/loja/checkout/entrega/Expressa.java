package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component("EXPRESSA")
public class Expressa implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("25.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("4.50");

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return TAXA_FIXA.add(TAXA_POR_KG.multiply(pesoTotalKg));
    }

    @Override
    public int prazoEntregaDias() {
        return 2;
    }
}
