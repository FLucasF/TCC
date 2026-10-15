package com.loja.checkout.domain.clube;

import com.loja.checkout.domain.Dinheiro;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE {
        @Override public BigDecimal credito(BigDecimal subtotalProdutos) { return Dinheiro.ZERO; }
        @Override public BigDecimal fretePago(BigDecimal freteBase) { return freteBase; }
        @Override public boolean temBrinde(BigDecimal subtotalProdutos) { return false; }
    },

    PRATA {
        @Override public BigDecimal credito(BigDecimal subtotalProdutos) {
            return Dinheiro.ajustar(subtotalProdutos.multiply(new BigDecimal("0.02")));
        }
        @Override public BigDecimal fretePago(BigDecimal freteBase) { return freteBase; }
        @Override public boolean temBrinde(BigDecimal subtotalProdutos) { return false; }
    },

    OURO {
        private final BigDecimal limiteBrinde = new BigDecimal("500");
        @Override public BigDecimal credito(BigDecimal subtotalProdutos) {
            return Dinheiro.ajustar(subtotalProdutos.multiply(new BigDecimal("0.05")));
        }
        @Override public BigDecimal fretePago(BigDecimal freteBase) { return Dinheiro.ZERO; }
        @Override public boolean temBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(limiteBrinde) > 0;
        }
    };

    public abstract BigDecimal credito(BigDecimal subtotalProdutos);
    public abstract BigDecimal fretePago(BigDecimal freteBase);
    public abstract boolean temBrinde(BigDecimal subtotalProdutos);
}
