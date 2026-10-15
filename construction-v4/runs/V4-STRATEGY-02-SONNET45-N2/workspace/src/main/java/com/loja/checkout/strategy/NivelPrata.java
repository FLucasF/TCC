package com.loja.checkout.strategy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class NivelPrata implements NivelClube {

    @Override
    public String getCodigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        BigDecimal credito = subtotalProdutos.multiply(new BigDecimal("0.02"));
        return credito.setScale(2, RoundingMode.HALF_EVEN);
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
