package com.loja.checkout.strategy;

import java.math.BigDecimal;

public class EntregaMotoboy implements Entrega {

    @Override
    public BigDecimal calcularCusto(Double pesoKg) {
        return new BigDecimal("18.00");
    }

    @Override
    public Integer prazo() {
        return 0;
    }

    @Override
    public boolean podeAtender(Double pesoKg) {
        return pesoKg <= 5.0;
    }

    @Override
    public String nome() {
        return "MOTOBOY";
    }

}
