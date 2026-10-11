package com.loja.checkout.dominio.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Prata implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal percentualCredito() {
        return PERCENTUAL_CREDITO;
    }

    @Override
    public boolean freteGratis() {
        return false;
    }

    @Override
    public boolean concedeBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
