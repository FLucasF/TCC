package com.loja.checkout.delivery;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RetiradaLojaDeliveryMethod implements DeliveryMethod {

    @Override
    public String getCodigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public int getPrazoDias() {
        return 1;
    }

    @Override
    public boolean isDisponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return BigDecimal.ZERO.setScale(2);
    }
}
