package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BeneficiosClube calcular(BigDecimal subtotalProdutos) {
        return new BeneficiosClube(BigDecimal.ZERO.setScale(2), false, false);
    }
}
