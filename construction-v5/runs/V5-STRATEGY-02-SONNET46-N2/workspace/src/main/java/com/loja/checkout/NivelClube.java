package com.loja.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum NivelClube {
    BRONZE {
        @Override public BigDecimal ajustarFrete(BigDecimal frete) { return frete; }
        @Override public BigDecimal calcularCredito(BigDecimal subtotal) { return BigDecimal.ZERO.setScale(2); }
        @Override public boolean ganhaBreinde(BigDecimal subtotal) { return false; }
    },
    PRATA {
        @Override public BigDecimal ajustarFrete(BigDecimal frete) { return frete; }
        @Override public BigDecimal calcularCredito(BigDecimal subtotal) {
            return arr(subtotal.multiply(new BigDecimal("0.02")));
        }
        @Override public boolean ganhaBreinde(BigDecimal subtotal) { return false; }
    },
    OURO {
        private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

        @Override public BigDecimal ajustarFrete(BigDecimal frete) { return BigDecimal.ZERO.setScale(2); }
        @Override public BigDecimal calcularCredito(BigDecimal subtotal) {
            return arr(subtotal.multiply(new BigDecimal("0.05")));
        }
        @Override public boolean ganhaBreinde(BigDecimal subtotal) {
            return subtotal.compareTo(MINIMO_BRINDE) > 0;
        }
    };

    public abstract BigDecimal ajustarFrete(BigDecimal frete);
    public abstract BigDecimal calcularCredito(BigDecimal subtotal);
    public abstract boolean ganhaBreinde(BigDecimal subtotal);

    private static BigDecimal arr(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }
}
