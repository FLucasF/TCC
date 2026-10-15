package com.loja.checkout.domain;

import static com.loja.checkout.domain.Dinheiro.centavos;

import java.math.BigDecimal;
import java.util.List;

/**
 * Formas de entrega. O que varia de uma para outra é o jeito de cobrar o
 * frete, o prazo e as limitações; por isso cada caso tem o seu próprio corpo.
 * Entrou uma transportadora nova? Basta acrescentar uma constante aqui.
 */
public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return centavos(BigDecimal.valueOf(12).add(BigDecimal.valueOf(2).multiply(pesoKg)));
        }
    },

    EXPRESSA(2) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return centavos(BigDecimal.valueOf(25).add(new BigDecimal("4.50").multiply(pesoKg)));
        }
    },

    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return centavos(BigDecimal.ZERO);
        }
    },

    MOTOBOY(0) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return centavos(BigDecimal.valueOf(18));
        }

        @Override
        public boolean disponivel(BigDecimal pesoKg) {
            return pesoKg.compareTo(BigDecimal.valueOf(5)) <= 0;
        }
    };

    private final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    public abstract BigDecimal frete(BigDecimal pesoKg);

    public boolean disponivel(BigDecimal pesoKg) {
        return true;
    }

    public int prazoDias() {
        return prazoDias;
    }

    public static BigDecimal pesoTotal(List<Item> itens) {
        return itens.stream()
                .map(Item::pesoLinha)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
