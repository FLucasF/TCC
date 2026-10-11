package com.loja.checkout.dominio.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Economica implements ModalidadeEntrega {

    private static final BigDecimal FIXO = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return FIXO.add(POR_KG.multiply(pesoKg));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
