package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal taxa;

    Regiao(String taxa) {
        this.taxa = new BigDecimal(taxa);
    }

    public BigDecimal calcularSeguro(BigDecimal subtotal) {
        return subtotal.multiply(taxa).setScale(2, RoundingMode.HALF_EVEN);
    }
}
