package com.loja.checkout.clube;

import java.math.BigDecimal;

public record BeneficiosClube(
        BigDecimal pctCashback,
        boolean freteGratis,
        BigDecimal limitePresente) {

    public boolean temBrinde(BigDecimal subtotal) {
        return limitePresente != null && subtotal.compareTo(limitePresente) > 0;
    }
}
