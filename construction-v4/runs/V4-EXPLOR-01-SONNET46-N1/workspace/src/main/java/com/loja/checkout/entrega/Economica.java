package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Economica implements ModalidadeEntrega {

    private static final BigDecimal TAXA_BASE = new BigDecimal("12.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return TAXA_BASE.add(TAXA_POR_KG.multiply(pesoKg));
    }

    @Override
    public int prazoDias() {
        return 7;
    }

    @Override
    public boolean disponivel(BigDecimal pesoKg) {
        return true;
    }
}
