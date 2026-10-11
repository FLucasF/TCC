package com.loja.checkout.dominio;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE(BigDecimal.ZERO),

    PRATA(new BigDecimal("0.02")),

    OURO(new BigDecimal("0.05")) {
        @Override
        public BigDecimal ajustarFrete(BigDecimal frete) {
            return Dinheiro.ZERO;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotal) {
            return subtotal.compareTo(LIMITE_BRINDE) > 0;
        }
    };

    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500");

    private final BigDecimal taxaCredito;

    NivelClube(BigDecimal taxaCredito) {
        this.taxaCredito = taxaCredito;
    }

    public BigDecimal ajustarFrete(BigDecimal frete) {
        return frete;
    }

    public boolean temBrinde(BigDecimal subtotal) {
        return false;
    }

    public BigDecimal credito(BigDecimal subtotal) {
        return Dinheiro.percentual(subtotal, taxaCredito);
    }
}
