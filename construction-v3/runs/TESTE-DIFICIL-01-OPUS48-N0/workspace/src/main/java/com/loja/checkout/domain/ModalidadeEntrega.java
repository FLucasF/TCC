package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Forma de entrega escolhida. Cada opcao sabe calcular o seu frete (a partir do
 * peso do pedido, sem arredondar), o seu prazo em dias, se atende um pedido de
 * determinado peso e se o envio tem seguro. Como quase toda semana entra uma
 * transportadora nova, basta adicionar uma constante aqui com o seu jeito de
 * cobrar, prazo e limitacoes.
 */
public enum ModalidadeEntrega {

    ECONOMICA {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg));
        }

        @Override
        public int prazoDias() {
            return 7;
        }
    },

    EXPRESSA {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg));
        }

        @Override
        public int prazoDias() {
            return 2;
        }
    },

    RETIRADA_LOJA {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return BigDecimal.ZERO;
        }

        @Override
        public int prazoDias() {
            return 1;
        }

        @Override
        public boolean temSeguro() {
            return false;
        }
    },

    MOTOBOY {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return new BigDecimal("18.00");
        }

        @Override
        public int prazoDias() {
            return 0;
        }

        @Override
        public boolean atende(BigDecimal pesoKg) {
            return pesoKg.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    /** Valor do frete (sem arredondar) para o peso informado. */
    public abstract BigDecimal frete(BigDecimal pesoKg);

    /** Prazo de entrega, em dias, desta modalidade. */
    public abstract int prazoDias();

    /** True quando a modalidade atende um pedido com este peso. */
    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    /** True quando o envio desta modalidade tem seguro. */
    public boolean temSeguro() {
        return true;
    }
}
