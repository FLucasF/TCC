package com.loja.checkout.domain.entrega.impl;

import com.loja.checkout.domain.entrega.ModalidadeEntregaStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RetiradaLoja implements ModalidadeEntregaStrategy {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public boolean atende(BigDecimal pesoKgTotal) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKgTotal) {
        return new BigDecimal("0.00");
    }

    @Override
    public int prazoEntregaDias() {
        return 1;
    }
}
