package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Formas de entrega. Cada opção define o frete, o prazo e quando está
 * disponível no seu próprio bloco. Para acrescentar uma transportadora nova,
 * basta mais uma constante aqui.
 */
public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg));
        }
    },
    EXPRESSA(2) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg));
        }
    },
    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return BigDecimal.ZERO;
        }
    },
    MOTOBOY(0) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return new BigDecimal("18.00");
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

    /** Frete da opção para o peso do pedido (ainda sem arredondar). */
    public abstract BigDecimal frete(BigDecimal pesoKg);

    public int prazoDias() {
        return prazoDias;
    }

    /** Se a opção atende um pedido com este peso. Por padrão, atende. */
    public boolean disponivel(BigDecimal pesoKg) {
        return true;
    }
}
