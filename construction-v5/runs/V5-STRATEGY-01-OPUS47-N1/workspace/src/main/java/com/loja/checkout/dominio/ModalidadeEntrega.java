package com.loja.checkout.dominio;

import com.loja.checkout.servico.Dinheiro;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA {
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return Dinheiro.arredondar(new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg)));
        }
        public int prazoDias() { return 7; }
        public boolean atende(BigDecimal pesoKg) { return true; }
    },
    EXPRESSA {
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return Dinheiro.arredondar(new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg)));
        }
        public int prazoDias() { return 2; }
        public boolean atende(BigDecimal pesoKg) { return true; }
    },
    RETIRADA_LOJA {
        public BigDecimal calcularFrete(BigDecimal pesoKg) { return Dinheiro.arredondar(BigDecimal.ZERO); }
        public int prazoDias() { return 1; }
        public boolean atende(BigDecimal pesoKg) { return true; }
    },
    MOTOBOY {
        public BigDecimal calcularFrete(BigDecimal pesoKg) { return Dinheiro.arredondar(new BigDecimal("18.00")); }
        public int prazoDias() { return 0; }
        public boolean atende(BigDecimal pesoKg) { return pesoKg.compareTo(new BigDecimal("5")) <= 0; }
    };

    public abstract BigDecimal calcularFrete(BigDecimal pesoKg);
    public abstract int prazoDias();
    public abstract boolean atende(BigDecimal pesoKg);
}
