package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ClubeOuro implements NivelClube {

    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    @Override
    public String getCodigo() {
        return "OURO";
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public BigDecimal ajustarFrete(BigDecimal frete) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(LIMITE_BRINDE) > 0;
    }
}
