package com.loja.checkout.domain.clube.impl;

import com.loja.checkout.domain.clube.NivelClubeStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class NivelOuro implements NivelClubeStrategy {

    private static final BigDecimal LIMIAR_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public boolean isentoFrete() {
        return true;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotal) {
        return subtotal.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean contemBrinde(BigDecimal subtotal) {
        return subtotal.compareTo(LIMIAR_BRINDE) > 0;
    }
}
