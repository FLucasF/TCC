package com.loja.checkout.entrega;

import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;
import java.util.Optional;

public enum ModalidadeEntrega {

    ECONOMICA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            BigDecimal base = new BigDecimal("12.00");
            BigDecimal porKg = new BigDecimal("2.00");
            return Dinheiro.arredondar(base.add(porKg.multiply(pesoKg)));
        }

        @Override
        public int prazoDias() {
            return 7;
        }

        @Override
        public boolean disponivel(BigDecimal pesoKg) {
            return true;
        }
    },

    EXPRESSA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            BigDecimal base = new BigDecimal("25.00");
            BigDecimal porKg = new BigDecimal("4.50");
            return Dinheiro.arredondar(base.add(porKg.multiply(pesoKg)));
        }

        @Override
        public int prazoDias() {
            return 2;
        }

        @Override
        public boolean disponivel(BigDecimal pesoKg) {
            return true;
        }
    },

    RETIRADA_LOJA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return new BigDecimal("0.00");
        }

        @Override
        public int prazoDias() {
            return 1;
        }

        @Override
        public boolean disponivel(BigDecimal pesoKg) {
            return true;
        }
    },

    MOTOBOY {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return new BigDecimal("18.00");
        }

        @Override
        public int prazoDias() {
            return 0;
        }

        @Override
        public boolean disponivel(BigDecimal pesoKg) {
            return pesoKg.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    public abstract BigDecimal calcularFrete(BigDecimal pesoKg);

    public abstract int prazoDias();

    public abstract boolean disponivel(BigDecimal pesoKg);

    public static Optional<ModalidadeEntrega> fromCodigo(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(codigo));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
