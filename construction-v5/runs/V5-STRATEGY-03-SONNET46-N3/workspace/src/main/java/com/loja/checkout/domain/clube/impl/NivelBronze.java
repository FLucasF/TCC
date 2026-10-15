package com.loja.checkout.domain.clube.impl;

import com.loja.checkout.domain.clube.NivelClubeStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class NivelBronze implements NivelClubeStrategy {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public boolean isentoFrete() {
        return false;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotal) {
        return new BigDecimal("0.00");
    }

    @Override
    public boolean contemBrinde(BigDecimal subtotal) {
        return false;
    }
}
