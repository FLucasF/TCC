package com.loja.checkout.service.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ClubeBronze implements BeneficioClube {

    @Override
    public String nivel() {
        return "BRONZE";
    }

    @Override
    public BigDecimal creditoProximaCompra(BigDecimal subtotal) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public boolean freteGratis() {
        return false;
    }

    @Override
    public boolean brinde(BigDecimal subtotal) {
        return false;
    }
}
