package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Opções de entrega. O enunciado diz que cada opção tem o seu próprio jeito de
 * cobrar, então o cálculo do frete é de cada constante — não é um percentual
 * em cima de uma conta comum, como no seguro. Entrar com uma transportadora
 * nova é acrescentar uma constante aqui.
 */
public enum ModalidadeEntrega {

    ECONOMICA(7) {
        private static final BigDecimal FIXO = new BigDecimal("12.00");
        private static final BigDecimal POR_KG = new BigDecimal("2.00");

        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.centavos(FIXO.add(POR_KG.multiply(pesoKg)));
        }
    },

    EXPRESSA(2) {
        private static final BigDecimal FIXO = new BigDecimal("25.00");
        private static final BigDecimal POR_KG = new BigDecimal("4.50");

        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.centavos(FIXO.add(POR_KG.multiply(pesoKg)));
        }
    },

    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.ZERO;
        }
    },

    MOTOBOY(0) {
        private static final BigDecimal VALOR = new BigDecimal("18.00");
        private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return VALOR;
        }

        @Override
        public boolean atende(BigDecimal pesoKg) {
            return pesoKg.compareTo(PESO_MAXIMO) <= 0;
        }
    };

    private final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    /** Frete desta opção para um pedido com este peso, em centavos. */
    public abstract BigDecimal frete(BigDecimal pesoKg);

    /** Se a opção atende um pedido com este peso. */
    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    public int prazoDias() {
        return prazoDias;
    }
}
