package com.loja.checkout.entrega;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class RetiradaLojaEntrega implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int prazoDias() {
        return 1;
    }
}
