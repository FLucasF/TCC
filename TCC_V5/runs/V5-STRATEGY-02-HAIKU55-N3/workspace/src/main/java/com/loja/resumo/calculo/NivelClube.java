package com.loja.resumo.calculo;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE("0.00"),
    PRATA("0.02"),
    OURO("0.05") {
        @Override
        BigDecimal freteCobrado(BigDecimal frete) {
            return Dinheiro.ZERO;
        }

        @Override
        boolean brinde(BigDecimal subtotal) {
            return subtotal.compareTo(new BigDecimal("500")) > 0;
        }
    };

    private final BigDecimal taxaCredito;

    NivelClube(String taxaCredito) {
        this.taxaCredito = new BigDecimal(taxaCredito);
    }

    BigDecimal credito(BigDecimal subtotal) {
        return Dinheiro.sobre(subtotal, taxaCredito);
    }

    BigDecimal freteCobrado(BigDecimal frete) {
        return frete;
    }

    boolean brinde(BigDecimal subtotal) {
        return false;
    }
}
