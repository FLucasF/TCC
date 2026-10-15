package com.loja.checkout.strategy;

import java.math.BigDecimal;

public class EntregaEconomica implements Entrega {

    @Override
    public BigDecimal calcularCusto(Double pesoKg) {
        return new BigDecimal("12.00").add(new BigDecimal(pesoKg).multiply(new BigDecimal("2.00")));
    }

    @Override
    public Integer prazo() {
        return 7;
    }

    @Override
    public boolean podeAtender(Double pesoKg) {
        return true;
    }

    @Override
    public String nome() {
        return "ECONOMICA";
    }

}
