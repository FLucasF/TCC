package com.loja.checkout.domain;

import java.math.BigDecimal;

import static com.loja.checkout.domain.Arredondamento.*;

public enum NivelClube {

    BRONZE {
        @Override public BigDecimal calcularCredito(BigDecimal subtotal) { return ZERO; }
        @Override public boolean freteGratis() { return false; }
        @Override public boolean darBrinde(BigDecimal subtotal) { return false; }
    },

    PRATA {
        @Override public BigDecimal calcularCredito(BigDecimal subtotal) {
            return r(subtotal.multiply(new BigDecimal("0.02")));
        }
        @Override public boolean freteGratis() { return false; }
        @Override public boolean darBrinde(BigDecimal subtotal) { return false; }
    },

    OURO {
        @Override public BigDecimal calcularCredito(BigDecimal subtotal) {
            return r(subtotal.multiply(new BigDecimal("0.05")));
        }
        @Override public boolean freteGratis() { return true; }
        @Override public boolean darBrinde(BigDecimal subtotal) {
            return subtotal.compareTo(new BigDecimal("500")) > 0;
        }
    };

    public abstract BigDecimal calcularCredito(BigDecimal subtotal);

    public abstract boolean freteGratis();

    public abstract boolean darBrinde(BigDecimal subtotal);
}
