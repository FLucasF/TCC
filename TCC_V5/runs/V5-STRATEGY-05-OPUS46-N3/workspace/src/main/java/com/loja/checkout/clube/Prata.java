package com.loja.checkout.clube;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class Prata implements NivelClube {

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(new BigDecimal("0.02"))
                .setScale(2, RoundingMode.HALF_EVEN);
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
