package com.loja.checkout.strategy;

import java.math.BigDecimal;

public class EntregaExpressa implements Entrega {

    @Override
    public BigDecimal calcularCusto(Double pesoKg) {
        return new BigDecimal("25.00").add(new BigDecimal(pesoKg).multiply(new BigDecimal("4.50")));
    }

    @Override
    public Integer prazo() {
        return 2;
    }

    @Override
    public boolean podeAtender(Double pesoKg) {
        return true;
    }

    @Override
    public String nome() {
        return "EXPRESSA";
    }

}
