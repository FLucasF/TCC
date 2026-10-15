package com.loja.checkout.service.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BronzeBeneficio implements NivelClubeBeneficio {

    @Override
    public String getCodigo() {
        return "BRONZE";
    }

    @Override
    public boolean freteGratis() {
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
