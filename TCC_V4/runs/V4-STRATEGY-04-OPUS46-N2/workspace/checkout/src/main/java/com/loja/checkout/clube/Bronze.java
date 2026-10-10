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
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return BigDecimal.ZERO;
    }

    @Override
    public boolean isFreteGratis() {
        return false;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
