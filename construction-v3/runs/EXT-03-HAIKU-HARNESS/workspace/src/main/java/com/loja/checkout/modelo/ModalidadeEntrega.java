package com.loja.checkout.modelo;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA(12, 2) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotal) {
            BigDecimal taxa = new BigDecimal("12.00");
            BigDecimal porKg = pesoTotal.multiply(new BigDecimal("2.00"));
            return taxa.add(porKg);
        }

        @Override
        public int getPrazo() {
            return 7;
        }

        @Override
        public boolean eDisponivel(BigDecimal pesoTotal) {
            return true;
        }
    },
    EXPRESSA(25, 4.5) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotal) {
            BigDecimal taxa = new BigDecimal("25.00");
            BigDecimal porKg = pesoTotal.multiply(new BigDecimal("4.50"));
            return taxa.add(porKg);
        }

        @Override
        public int getPrazo() {
            return 2;
        }

        @Override
        public boolean eDisponivel(BigDecimal pesoTotal) {
            return true;
        }
    },
    RETIRADA_LOJA(0, 0) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotal) {
            return BigDecimal.ZERO;
        }

        @Override
        public int getPrazo() {
            return 1;
        }

        @Override
        public boolean eDisponivel(BigDecimal pesoTotal) {
            return true;
        }
    },
    MOTOBOY(18, 0) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotal) {
            return new BigDecimal("18.00");
        }

        @Override
        public int getPrazo() {
            return 0;
        }

        @Override
        public boolean eDisponivel(BigDecimal pesoTotal) {
            return pesoTotal.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    private final int baseOuTaxa;
    private final double porKg;

    ModalidadeEntrega(int baseOuTaxa, double porKg) {
        this.baseOuTaxa = baseOuTaxa;
        this.porKg = porKg;
    }

    public abstract BigDecimal calcularFrete(BigDecimal pesoTotal);
    public abstract int getPrazo();
    public abstract boolean eDisponivel(BigDecimal pesoTotal);

    public static ModalidadeEntrega fromString(String codigo) {
        if (codigo == null) {
            return null;
        }
        try {
            return ModalidadeEntrega.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
