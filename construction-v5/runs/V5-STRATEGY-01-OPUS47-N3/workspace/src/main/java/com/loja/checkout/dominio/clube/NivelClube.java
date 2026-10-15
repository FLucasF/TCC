package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE {
        @Override public BigDecimal cashback(BigDecimal produtos) { return Dinheiro.ZERO; }
        @Override public BigDecimal ajustaFrete(BigDecimal frete) { return frete; }
        @Override public boolean brinde(BigDecimal produtos) { return false; }
    },
    PRATA {
        @Override public BigDecimal cashback(BigDecimal produtos) {
            return Dinheiro.aCentavos(produtos.multiply(new BigDecimal("0.02")));
        }
        @Override public BigDecimal ajustaFrete(BigDecimal frete) { return frete; }
        @Override public boolean brinde(BigDecimal produtos) { return false; }
    },
    OURO {
        @Override public BigDecimal cashback(BigDecimal produtos) {
            return Dinheiro.aCentavos(produtos.multiply(new BigDecimal("0.05")));
        }
        @Override public BigDecimal ajustaFrete(BigDecimal frete) { return Dinheiro.ZERO; }
        @Override public boolean brinde(BigDecimal produtos) {
            return produtos.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    public abstract BigDecimal cashback(BigDecimal produtos);
    public abstract BigDecimal ajustaFrete(BigDecimal frete);
    public abstract boolean brinde(BigDecimal produtos);
}
