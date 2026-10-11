package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ClubePrata implements NivelClube {

    @Override
    public String getCodigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public BigDecimal ajustarFrete(BigDecimal frete) {
        return frete;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
