package com.loja.checkout.domain.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class Prata implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public boolean freteGratis() {
        return false;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(PERCENTUAL_CREDITO).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
