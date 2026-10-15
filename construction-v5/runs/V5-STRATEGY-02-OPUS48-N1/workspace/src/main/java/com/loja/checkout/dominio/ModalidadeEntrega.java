package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Forma de entrega. Cada opção tem seu jeito de cobrar o frete, seu prazo e
 * suas limitações, e tudo isso mora na própria opção. Uma transportadora nova
 * entra como uma constante nova, sem mexer nas outras nem no cálculo geral.
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
        public boolean atende(BigDecimal pesoKg) {
            return pesoKg.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    private final int prazoEntregaDias;

    ModalidadeEntrega(int prazoEntregaDias) {
        this.prazoEntregaDias = prazoEntregaDias;
    }

    public abstract BigDecimal frete(BigDecimal pesoKg);

    /** Se a opção existe mas não serve para este pedido (ex.: motoboy acima de 5 kg). */
    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    public int prazoEntregaDias() {
        return prazoEntregaDias;
    }
}
