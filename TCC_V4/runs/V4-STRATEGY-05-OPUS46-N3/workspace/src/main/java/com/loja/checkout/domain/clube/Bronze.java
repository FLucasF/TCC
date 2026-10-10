package com.loja.checkout.domain.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Bronze implements NivelClube {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return ZERO;
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
