package com.loja.checkout.clube;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class Bronze implements NivelClube {

    @Override
    public String getCodigo() {
        return "BRONZE";
    }

    @Override
    public boolean isFreteGratis() {
        return false;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
