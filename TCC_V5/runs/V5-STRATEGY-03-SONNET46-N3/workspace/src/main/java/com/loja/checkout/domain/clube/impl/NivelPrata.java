package com.loja.checkout.domain.clube.impl;

import com.loja.checkout.domain.clube.NivelClubeStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class NivelPrata implements NivelClubeStrategy {

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public boolean isentoFrete() {
        return false;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotal) {
        return subtotal.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean contemBrinde(BigDecimal subtotal) {
        return false;
    }
}
