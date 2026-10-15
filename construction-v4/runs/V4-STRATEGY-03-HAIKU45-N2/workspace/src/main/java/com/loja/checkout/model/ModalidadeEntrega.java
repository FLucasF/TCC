package com.loja.checkout.model;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA(new BigDecimal("12.00"), new BigDecimal("2.00"), 7) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotal) {
            return valor.add(pesoTotal.multiply(valorPorKg));
        }
    },
    EXPRESSA(new BigDecimal("25.00"), new BigDecimal("4.50"), 2) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotal) {
            return valor.add(pesoTotal.multiply(valorPorKg));
        }
    },
    RETIRADA_LOJA(BigDecimal.ZERO, BigDecimal.ZERO, 1) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotal) {
            return BigDecimal.ZERO;
        }
    },
    MOTOBOY(new BigDecimal("18.00"), BigDecimal.ZERO, 0) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotal) {
            return valor;
        }
    };

    protected final BigDecimal valor;
    protected final BigDecimal valorPorKg;
    private final int prazo;

    ModalidadeEntrega(BigDecimal valor, BigDecimal valorPorKg, int prazo) {
        this.valor = valor;
        this.valorPorKg = valorPorKg;
        this.prazo = prazo;
    }

    public abstract BigDecimal calcularFrete(BigDecimal pesoTotal);

    public int getPrazo() {
        return prazo;
    }

    public boolean atende(BigDecimal pesoTotal) {
        if (this == MOTOBOY) {
            return pesoTotal.compareTo(new BigDecimal("5")) <= 0;
        }
        return true;
    }
}
