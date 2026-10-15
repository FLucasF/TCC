package com.loja.checkout.clube;

import java.math.BigDecimal;

public class ClubeBronze implements BeneficiosClube {
    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return new BigDecimal("0.00");
    }

    @Override
    public boolean temFretGratis() {
        return false;
    }

    @Override
    public boolean temBrindeAcima500() {
        return false;
    }
}
