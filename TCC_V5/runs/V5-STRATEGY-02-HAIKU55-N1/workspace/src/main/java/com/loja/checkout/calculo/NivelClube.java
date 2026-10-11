package com.loja.checkout.calculo;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE(new BigDecimal("0")),

    PRATA(new BigDecimal("0.02")),

    OURO(new BigDecimal("0.05")) {
        BigDecimal freteFinal(BigDecimal freteBase) {
            return BigDecimal.ZERO;
        }

        boolean temBrinde(BigDecimal subtotal) {
            return subtotal.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    private final BigDecimal taxaCredito;

    NivelClube(BigDecimal taxaCredito) {
        this.taxaCredito = taxaCredito;
    }

    BigDecimal creditoPara(BigDecimal subtotal) {
        return Dinheiro.arredondar(subtotal.multiply(taxaCredito));
    }

    BigDecimal freteFinal(BigDecimal freteBase) {
        return freteBase;
    }

    boolean temBrinde(BigDecimal subtotal) {
        return false;
    }
}
