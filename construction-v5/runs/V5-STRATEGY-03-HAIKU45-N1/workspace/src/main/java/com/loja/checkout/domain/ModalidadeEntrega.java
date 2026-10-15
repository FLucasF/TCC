package com.loja.checkout.domain;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA(BigDecimal.valueOf(12), BigDecimal.valueOf(2), 7, 999) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotal) {
            return precoBase.add(valorPorKg.multiply(pesoTotal));
        }
    },
    EXPRESSA(BigDecimal.valueOf(25), BigDecimal.valueOf(4.50), 2, 999) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotal) {
            return precoBase.add(valorPorKg.multiply(pesoTotal));
        }
    },
    RETIRADA_LOJA(BigDecimal.ZERO, BigDecimal.ZERO, 1, 999) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotal) {
            return BigDecimal.ZERO;
        }
    },
    MOTOBOY(BigDecimal.valueOf(18), BigDecimal.ZERO, 0, 5) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotal) {
            return BigDecimal.valueOf(18);
        }
    };

    protected final BigDecimal precoBase;
    protected final BigDecimal valorPorKg;
    protected final int prazoEntregaDias;
    protected final int pesoMaximoKg;

    ModalidadeEntrega(BigDecimal precoBase, BigDecimal valorPorKg, int prazoEntregaDias, int pesoMaximoKg) {
        this.precoBase = precoBase;
        this.valorPorKg = valorPorKg;
        this.prazoEntregaDias = prazoEntregaDias;
        this.pesoMaximoKg = pesoMaximoKg;
    }

    public abstract BigDecimal calcularFrete(BigDecimal pesoTotal);

    public int getPrazoEntregaDias() {
        return prazoEntregaDias;
    }

    public int getPesoMaximoKg() {
        return pesoMaximoKg;
    }

    public static ModalidadeEntrega fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return ModalidadeEntrega.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
