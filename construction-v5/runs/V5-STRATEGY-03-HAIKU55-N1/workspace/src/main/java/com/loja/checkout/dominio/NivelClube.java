package com.loja.checkout.dominio;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE("0.00"),
    PRATA("0.02"),
    OURO("0.05") {
        @Override
        public BigDecimal ajustarFrete(BigDecimal frete) {
            return Dinheiro.ZERO;
        }

        @Override
        public boolean temBrinde(Pedido pedido) {
            return pedido.subtotal().compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    private final String taxaCredito;

    NivelClube(String taxaCredito) {
        this.taxaCredito = taxaCredito;
    }

    public BigDecimal credito(Pedido pedido) {
        return Dinheiro.percentual(pedido.subtotal(), taxaCredito);
    }

    public BigDecimal ajustarFrete(BigDecimal frete) {
        return frete;
    }

    public boolean temBrinde(Pedido pedido) {
        return false;
    }
}
