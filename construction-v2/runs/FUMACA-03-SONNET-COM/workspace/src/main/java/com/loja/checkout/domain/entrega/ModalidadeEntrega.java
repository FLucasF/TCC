package com.loja.checkout.domain.entrega;

import com.loja.checkout.domain.Dinheiro;

import java.math.BigDecimal;

/**
 * Cada opção de entrega decide seu próprio custo, prazo e disponibilidade.
 * Uma opção nova entra como uma nova constante, sem tocar nas existentes.
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
        public boolean disponivelPara(BigDecimal pesoTotalKg) {
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
        public boolean disponivelPara(BigDecimal pesoTotalKg) {
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
        public boolean disponivelPara(BigDecimal pesoTotalKg) {
            return true;
        }
    },

    MOTOBOY {
        private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return Dinheiro.arredondar(new BigDecimal("18.00"));
        }

        @Override
        public int prazoDias() {
            return 0;
        }

        @Override
        public boolean disponivelPara(BigDecimal pesoTotalKg) {
            return pesoTotalKg.compareTo(PESO_MAXIMO_KG) <= 0;
        }
    };

    public abstract BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    public abstract int prazoDias();

    public abstract boolean disponivelPara(BigDecimal pesoTotalKg);
}
