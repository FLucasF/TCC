package com.loja.resumo.dominio;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE("0"),
    PRATA("0.02"),
    OURO("0.05") {
        @Override
        public BigDecimal ajustarFrete(BigDecimal frete) {
            return BigDecimal.ZERO;
        }

        @Override
        public boolean recebeBrinde(BigDecimal subtotal) {
            return subtotal.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    private final BigDecimal percentualCredito;

    NivelClube(String percentualCredito) {
        this.percentualCredito = new BigDecimal(percentualCredito);
    }

    public BigDecimal creditoSobre(BigDecimal subtotal) {
        return subtotal.multiply(percentualCredito);
    }

    public BigDecimal ajustarFrete(BigDecimal frete) {
        return frete;
    }

    public boolean recebeBrinde(BigDecimal subtotal) {
        return false;
    }
}
