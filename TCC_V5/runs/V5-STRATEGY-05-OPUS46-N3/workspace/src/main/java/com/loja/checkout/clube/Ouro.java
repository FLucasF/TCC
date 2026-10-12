package com.loja.checkout.clube;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class Ouro implements NivelClube {

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(new BigDecimal("0.05"))
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean freteGratis() {
        return true;
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
    }
}
