package com.loja.checkout.dominio;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE("0"),
    PRATA("0.02"),
    OURO("0.05") {
        public BigDecimal aplicarFrete(BigDecimal frete) {
            return Dinheiro.ZERO;
        }

        public boolean temBrinde(BigDecimal subtotal) {
            return subtotal.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    private final String percentualCredito;

    NivelClube(String percentualCredito) {
        this.percentualCredito = percentualCredito;
    }

    public BigDecimal creditoSobre(BigDecimal subtotal) {
        return Dinheiro.porcentagem(subtotal, percentualCredito);
    }

    public BigDecimal aplicarFrete(BigDecimal frete) {
        return frete;
    }

    public boolean temBrinde(BigDecimal subtotal) {
        return false;
    }
}
