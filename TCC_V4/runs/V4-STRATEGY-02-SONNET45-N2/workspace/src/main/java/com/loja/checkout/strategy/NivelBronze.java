package com.loja.checkout.strategy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class NivelBronze implements NivelClube {

    @Override
    public String getCodigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public BigDecimal ajustarFrete(BigDecimal frete) {
        return frete;
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
