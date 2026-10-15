package com.loja.checkout.club;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BronzeClubLevel implements ClubLevel {

    @Override
    public String getCodigo() {
        return "BRONZE";
    }

    @Override
    public boolean isFreteGratis() {
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
