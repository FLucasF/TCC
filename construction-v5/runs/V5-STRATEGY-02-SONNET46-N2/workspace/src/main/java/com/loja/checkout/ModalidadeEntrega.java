package com.loja.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum ModalidadeEntrega {
    ECONOMICA {
        @Override public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return arr(new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg)));
        }
        @Override public int prazo() { return 7; }
        @Override public boolean aceita(BigDecimal pesoKg) { return true; }
    },
    EXPRESSA {
        @Override public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return arr(new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg)));
        }
        @Override public int prazo() { return 2; }
        @Override public boolean aceita(BigDecimal pesoKg) { return true; }
    },
    RETIRADA_LOJA {
        @Override public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return BigDecimal.ZERO.setScale(2);
        }
        @Override public int prazo() { return 1; }
        @Override public boolean aceita(BigDecimal pesoKg) { return true; }
    },
    MOTOBOY {
        private static final BigDecimal LIMITE_KG = new BigDecimal("5");

        @Override public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return new BigDecimal("18.00");
        }
        @Override public int prazo() { return 0; }
        @Override public boolean aceita(BigDecimal pesoKg) {
            return pesoKg.compareTo(LIMITE_KG) <= 0;
        }
    };

    public abstract BigDecimal calcularFrete(BigDecimal pesoKg);
    public abstract int prazo();
    public abstract boolean aceita(BigDecimal pesoKg);

    private static BigDecimal arr(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }
}
