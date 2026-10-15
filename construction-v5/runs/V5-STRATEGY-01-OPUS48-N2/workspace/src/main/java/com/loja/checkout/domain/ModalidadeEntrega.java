package com.loja.checkout.domain;

import com.loja.checkout.dinheiro.Dinheiro;

import java.math.BigDecimal;

/**
 * Formas de entrega. O que muda de uma para outra — como cobra o frete, o
 * prazo e se atende o pedido — mora em cada constante. Entrar uma transportadora
 * nova é acrescentar uma constante aqui, sem mexer em quem escolhe a modalidade.
 */
public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.arredondar(new BigDecimal("12").add(new BigDecimal("2").multiply(pesoKg)));
        }
    },

    EXPRESSA(2) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.arredondar(new BigDecimal("25").add(new BigDecimal("4.50").multiply(pesoKg)));
        }
    },

    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.arredondar(BigDecimal.ZERO);
        }
    },

    MOTOBOY(0) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.arredondar(new BigDecimal("18"));
        }

        @Override
        public boolean disponivel(BigDecimal pesoKg) {
            return pesoKg.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    private final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    public int prazoDias() {
        return prazoDias;
    }

    /** Frete da modalidade para o peso do pedido, antes de vantagens do clube. */
    public abstract BigDecimal frete(BigDecimal pesoKg);

    /** Se a modalidade atende um pedido com este peso. */
    public boolean disponivel(BigDecimal pesoKg) {
        return true;
    }
}
