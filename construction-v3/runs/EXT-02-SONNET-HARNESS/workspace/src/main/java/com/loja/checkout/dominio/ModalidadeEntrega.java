package com.loja.checkout.dominio;

import java.math.BigDecimal;

public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        public BigDecimal custo(BigDecimal pesoTotalKg) {
            return Dinheiro.arredondar(new BigDecimal("12.00")
                    .add(new BigDecimal("2.00").multiply(pesoTotalKg)));
        }
    },

    EXPRESSA(2) {
        @Override
        public BigDecimal custo(BigDecimal pesoTotalKg) {
            return Dinheiro.arredondar(new BigDecimal("25.00")
                    .add(new BigDecimal("4.50").multiply(pesoTotalKg)));
        }
    },

    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal custo(BigDecimal pesoTotalKg) {
            return Dinheiro.arredondar(BigDecimal.ZERO);
        }
    },

    MOTOBOY(0) {
        private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

        @Override
        public BigDecimal custo(BigDecimal pesoTotalKg) {
            return Dinheiro.arredondar(new BigDecimal("18.00"));
        }

        @Override
        public boolean disponivel(BigDecimal pesoTotalKg) {
            return pesoTotalKg.compareTo(PESO_MAXIMO_KG) <= 0;
        }
    };

    private final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    public abstract BigDecimal custo(BigDecimal pesoTotalKg);

    public boolean disponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    public int prazoDias() {
        return prazoDias;
    }
}
