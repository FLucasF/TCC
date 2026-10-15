package com.loja.checkout.domain.clube;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class Prata implements NivelClube {

    private static final BigDecimal CASHBACK = new BigDecimal("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(CASHBACK);
    }

    @Override
    public BigDecimal freteAplicado(BigDecimal frete) {
        return frete;
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
