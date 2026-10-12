package com.loja.checkout.calculo;

import java.math.BigDecimal;

import static com.loja.checkout.calculo.Dinheiro.ZERO;
import static com.loja.checkout.calculo.Dinheiro.centavos;

enum NivelClube {

    BRONZE(new BigDecimal("0.00")),
    PRATA(new BigDecimal("0.02")),
    OURO(new BigDecimal("0.05")) {
        @Override
        BigDecimal freteCobrado(BigDecimal frete) {
            return ZERO;
        }

        @Override
        boolean temBrinde(BigDecimal subtotal) {
            return subtotal.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    private final BigDecimal percentualCredito;

    NivelClube(BigDecimal percentualCredito) {
        this.percentualCredito = percentualCredito;
    }

    BigDecimal freteCobrado(BigDecimal frete) {
        return frete;
    }

    boolean temBrinde(BigDecimal subtotal) {
        return false;
    }

    BigDecimal credito(BigDecimal subtotal) {
        return centavos(subtotal.multiply(percentualCredito));
    }
}
