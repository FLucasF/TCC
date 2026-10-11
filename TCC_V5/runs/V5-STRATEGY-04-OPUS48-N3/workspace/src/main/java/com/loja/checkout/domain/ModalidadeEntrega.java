package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/**
 * Cada modalidade de entrega tem seu jeito de cobrar, seu prazo e suas
 * limitações. Nova parceria = nova constante aqui, sem mexer no resto.
 */
public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.arredonda(new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg)));
        }
    },
    EXPRESSA(2) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.arredonda(new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg)));
        }
    },
    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.ZERO;
        }
    },
    MOTOBOY(0) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return new BigDecimal("18.00");
        }

        @Override
        public boolean atende(BigDecimal pesoKg) {
            return pesoKg.compareTo(LIMITE_MOTOBOY_KG) <= 0;
        }
    };

    private static final BigDecimal LIMITE_MOTOBOY_KG = new BigDecimal("5");

    private final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    public abstract BigDecimal frete(BigDecimal pesoKg);

    /** A modalidade atende este peso de pedido? Por padrão, sim. */
    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    public int prazoDias() {
        return prazoDias;
    }

    public static Optional<ModalidadeEntrega> fromCodigo(String codigo) {
        return Arrays.stream(values()).filter(m -> m.name().equals(codigo)).findFirst();
    }
}
