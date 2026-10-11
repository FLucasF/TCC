package com.loja.resumocompra;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE("0"),

    PRATA("0.02"),

    OURO("0.05") {
        @Override
        public BigDecimal aplicarFrete(BigDecimal frete) {
            return BigDecimal.ZERO;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotal) {
            return subtotal.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    private final BigDecimal taxaCredito;

    NivelClube(String taxaCredito) {
        this.taxaCredito = new BigDecimal(taxaCredito);
    }

    public BigDecimal taxaCredito() {
        return taxaCredito;
    }

    public BigDecimal aplicarFrete(BigDecimal frete) {
        return frete;
    }

    public boolean temBrinde(BigDecimal subtotal) {
        return false;
    }
}
