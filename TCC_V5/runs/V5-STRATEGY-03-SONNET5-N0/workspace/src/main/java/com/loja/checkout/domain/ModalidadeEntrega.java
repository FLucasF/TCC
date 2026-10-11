package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Opcoes de entrega. Como novas transportadoras entram com frequencia, cada opcao
 * concentra sua propria regra de custo, prazo e disponibilidade em um unico lugar,
 * de forma que adicionar uma opcao nova nao exige tocar nas demais.
 */
public enum ModalidadeEntrega {

    ECONOMICA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return Dinheiro.arredondar(new BigDecimal("12.00")
                    .add(new BigDecimal("2.00").multiply(pesoTotalKg)));
        }

        @Override
        public int prazoDias() {
            return 7;
        }

        @Override
        public boolean disponivel(BigDecimal pesoTotalKg) {
            return true;
        }
    },

    EXPRESSA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return Dinheiro.arredondar(new BigDecimal("25.00")
                    .add(new BigDecimal("4.50").multiply(pesoTotalKg)));
        }

        @Override
        public int prazoDias() {
            return 2;
        }

        @Override
        public boolean disponivel(BigDecimal pesoTotalKg) {
            return true;
        }
    },

    RETIRADA_LOJA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return Dinheiro.arredondar(BigDecimal.ZERO);
        }

        @Override
        public int prazoDias() {
            return 1;
        }

        @Override
        public boolean disponivel(BigDecimal pesoTotalKg) {
            return true;
        }
    },

    MOTOBOY {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return Dinheiro.arredondar(new BigDecimal("18.00"));
        }

        @Override
        public int prazoDias() {
            return 0;
        }

        @Override
        public boolean disponivel(BigDecimal pesoTotalKg) {
            return pesoTotalKg.compareTo(MOTOBOY_LIMITE_PESO_KG) <= 0;
        }
    };

    private static final BigDecimal MOTOBOY_LIMITE_PESO_KG = new BigDecimal("5");

    public abstract BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    public abstract int prazoDias();

    public abstract boolean disponivel(BigDecimal pesoTotalKg);
}
