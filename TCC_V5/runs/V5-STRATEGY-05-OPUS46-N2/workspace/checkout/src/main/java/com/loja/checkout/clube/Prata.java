package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Prata implements NivelClube {

    private static final BigDecimal CASHBACK = new BigDecimal("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal taxaCashback() {
        return CASHBACK;
    }

    @Override
    public boolean freteGratis() {
        return false;
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
