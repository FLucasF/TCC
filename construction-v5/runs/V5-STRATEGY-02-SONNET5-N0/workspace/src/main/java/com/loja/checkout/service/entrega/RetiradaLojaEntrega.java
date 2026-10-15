package com.loja.checkout.service.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RetiradaLojaEntrega implements EntregaCalculadora {

    @Override
    public String getCodigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotalKg, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int getPrazoDias() {
        return 1;
    }
}
