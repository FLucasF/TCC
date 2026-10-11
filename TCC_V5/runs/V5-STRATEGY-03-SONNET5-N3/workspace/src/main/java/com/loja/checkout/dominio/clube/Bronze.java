package com.loja.checkout.dominio.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal percentualCredito() {
        return BigDecimal.ZERO;
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
