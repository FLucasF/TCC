package com.loja.checkout.domain.entrega.impl;

import com.loja.checkout.domain.entrega.ModalidadeEntregaStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Motoboy implements ModalidadeEntregaStrategy {

    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean atende(BigDecimal pesoKgTotal) {
        return pesoKgTotal.compareTo(PESO_MAXIMO) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKgTotal) {
        return new BigDecimal("18.00");
    }

    @Override
    public int prazoEntregaDias() {
        return 0;
    }
}
