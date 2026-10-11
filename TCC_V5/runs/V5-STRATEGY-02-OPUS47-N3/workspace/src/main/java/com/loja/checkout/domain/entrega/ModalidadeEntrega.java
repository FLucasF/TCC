package com.loja.checkout.domain.entrega;

import com.loja.checkout.domain.Dinheiro;

import java.math.BigDecimal;

public enum ModalidadeEntrega {

    ECONOMICA {
        @Override public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.ajustar(new BigDecimal("12").add(new BigDecimal("2").multiply(pesoKg)));
        }
        @Override public int prazoDias() { return 7; }
        @Override public boolean atende(BigDecimal pesoKg) { return true; }
    },

    EXPRESSA {
        @Override public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.ajustar(new BigDecimal("25").add(new BigDecimal("4.50").multiply(pesoKg)));
        }
        @Override public int prazoDias() { return 2; }
        @Override public boolean atende(BigDecimal pesoKg) { return true; }
    },

    RETIRADA_LOJA {
        @Override public BigDecimal frete(BigDecimal pesoKg) { return Dinheiro.ZERO; }
        @Override public int prazoDias() { return 1; }
        @Override public boolean atende(BigDecimal pesoKg) { return true; }
    },

    MOTOBOY {
        private final BigDecimal limite = new BigDecimal("5");
        @Override public BigDecimal frete(BigDecimal pesoKg) { return Dinheiro.ajustar(new BigDecimal("18.00")); }
        @Override public int prazoDias() { return 0; }
        @Override public boolean atende(BigDecimal pesoKg) { return pesoKg.compareTo(limite) <= 0; }
    };

    public abstract BigDecimal frete(BigDecimal pesoKg);
    public abstract int prazoDias();
    public abstract boolean atende(BigDecimal pesoKg);
}
