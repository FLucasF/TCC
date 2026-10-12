package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RetiradaLojaStrategy implements ModalidadeEntregaStrategy {

    @Override
    public String getCodigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return BigDecimal.ZERO;
    }

    @Override
    public int getPrazoDias() {
        return 1;
    }

    @Override
    public boolean atende(BigDecimal pesoTotalKg) {
        return true;
    }
}
