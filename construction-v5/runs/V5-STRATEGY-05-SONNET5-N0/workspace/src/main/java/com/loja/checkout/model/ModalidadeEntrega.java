package com.loja.checkout.model;

import java.math.BigDecimal;

public enum ModalidadeEntrega {

    ECONOMICA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg));
        }

        @Override
        public int prazoEntregaDias() {
            return 7;
        }
    },
    EXPRESSA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg));
        }

        @Override
        public int prazoEntregaDias() {
            return 2;
        }
    },
    RETIRADA_LOJA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return BigDecimal.ZERO;
        }

        @Override
        public int prazoEntregaDias() {
            return 1;
        }
    },
    MOTOBOY {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return new BigDecimal("18.00");
        }

        @Override
        public int prazoEntregaDias() {
            return 0;
        }

        @Override
        public boolean disponivelPara(BigDecimal pesoKg) {
            return pesoKg.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    public abstract BigDecimal calcularFrete(BigDecimal pesoKg);

    public abstract int prazoEntregaDias();

    public boolean disponivelPara(BigDecimal pesoKg) {
        return true;
    }
}
