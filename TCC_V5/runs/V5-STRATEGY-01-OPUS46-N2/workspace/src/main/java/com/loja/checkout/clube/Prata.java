package com.loja.checkout.clube;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class Prata implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public String getCodigo() {
        return "PRATA";
    }

    @Override
    public boolean isFreteGratis() {
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
