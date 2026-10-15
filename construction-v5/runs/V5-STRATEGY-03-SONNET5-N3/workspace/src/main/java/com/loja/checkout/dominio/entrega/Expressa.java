package com.loja.checkout.dominio.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Expressa implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("25.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("4.50");

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public int prazoDias() {
        return 2;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return TAXA_FIXA.add(TAXA_POR_KG.multiply(pesoKg));
    }
}
