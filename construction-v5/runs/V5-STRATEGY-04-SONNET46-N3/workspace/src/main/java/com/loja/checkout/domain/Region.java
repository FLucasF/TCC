package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum Region {
    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));

    private final BigDecimal rate;

    Region(BigDecimal rate) {
        this.rate = rate;
    }

    public BigDecimal insuranceOn(BigDecimal subtotal) {
        return subtotal.multiply(rate).setScale(2, RoundingMode.HALF_EVEN);
    }
}
