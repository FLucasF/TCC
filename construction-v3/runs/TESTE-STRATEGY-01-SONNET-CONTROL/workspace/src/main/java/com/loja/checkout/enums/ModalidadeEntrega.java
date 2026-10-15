package com.loja.checkout.enums;

import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;

/**
 * Cada opção de entrega define seu próprio custo, prazo e regra de disponibilidade.
 * Novas transportadoras entram como uma nova constante do enum.
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
    },

    RETIRADA_LOJA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return BigDecimal.ZERO.setScale(2);
        }

        @Override
        public int prazoDias() {
            return 1;
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
        public boolean disponivel(BigDecimal pesoTotalKg) {
            return pesoTotalKg.compareTo(PESO_MAXIMO_KG) <= 0;
        }
    };

    public abstract BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    public abstract int prazoDias();

    public boolean disponivel(BigDecimal pesoTotalKg) {
        return true;
    }
}
