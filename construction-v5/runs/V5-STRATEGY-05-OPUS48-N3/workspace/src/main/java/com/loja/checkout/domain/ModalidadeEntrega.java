package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Formas de entrega. Cada uma define sua fórmula de frete, seu prazo e sua
 * disponibilidade. Uma transportadora nova é uma constante nova aqui.
 */
public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        public BigDecimal freteBase(BigDecimal pesoKg) {
            return new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg));
        }
    },
    EXPRESSA(2) {
        @Override
        public BigDecimal freteBase(BigDecimal pesoKg) {
            return new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg));
        }
    },
    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal freteBase(BigDecimal pesoKg) {
            return BigDecimal.ZERO;
        }
    },
    MOTOBOY(0) {
        @Override
        public BigDecimal freteBase(BigDecimal pesoKg) {
            return new BigDecimal("18.00");
        }

        @Override
        public boolean disponivelPara(BigDecimal pesoKg) {
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

    /** Frete antes do arredondamento; o serviço arredonda para centavos. */
    public abstract BigDecimal freteBase(BigDecimal pesoKg);

    /** Se esta modalidade atende um pedido com este peso. */
    public boolean disponivelPara(BigDecimal pesoKg) {
        return true;
    }
}
