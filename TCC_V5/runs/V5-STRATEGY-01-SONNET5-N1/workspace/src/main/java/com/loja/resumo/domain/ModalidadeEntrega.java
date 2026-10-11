package com.loja.resumo.domain;

import java.math.BigDecimal;

/**
 * Cada modalidade tem seu próprio custo, prazo e condição de disponibilidade.
 * Nova parceria de entrega = nova constante, sem tocar nas demais.
 */
public enum ModalidadeEntrega {

    ECONOMICA {
        @Override
        public boolean disponivelPara(BigDecimal pesoTotalKg) {
            return true;
        }

        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return Dinheiro.arredondar(new BigDecimal("12.00")
                    .add(new BigDecimal("2.00").multiply(pesoTotalKg)));
        }

        @Override
        public int prazoDias() {
            return 7;
        }
    },

    EXPRESSA {
        @Override
        public boolean disponivelPara(BigDecimal pesoTotalKg) {
            return true;
        }

        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return Dinheiro.arredondar(new BigDecimal("25.00")
                    .add(new BigDecimal("4.50").multiply(pesoTotalKg)));
        }

        @Override
        public int prazoDias() {
            return 2;
        }
    },

    RETIRADA_LOJA {
        @Override
        public boolean disponivelPara(BigDecimal pesoTotalKg) {
            return true;
        }

        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return Dinheiro.arredondar(BigDecimal.ZERO);
        }

        @Override
        public int prazoDias() {
            return 1;
        }
    },

    MOTOBOY {
        @Override
        public boolean disponivelPara(BigDecimal pesoTotalKg) {
            return pesoTotalKg.compareTo(MOTOBOY_LIMITE_KG) <= 0;
        }

        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return Dinheiro.arredondar(new BigDecimal("18.00"));
        }

        @Override
        public int prazoDias() {
            return 0;
        }
    };

    private static final BigDecimal MOTOBOY_LIMITE_KG = new BigDecimal("5");

    public abstract boolean disponivelPara(BigDecimal pesoTotalKg);

    public abstract BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    public abstract int prazoDias();
}
