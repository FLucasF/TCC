package com.loja.checkout.domain;

import com.loja.checkout.exception.CheckoutException;
import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return new BigDecimal("12.00")
                .add(new BigDecimal("2.00").multiply(pesoTotalKg))
                .setScale(2, java.math.RoundingMode.HALF_EVEN);
        }

        @Override
        public int getPrazoEntregaDias() {
            return 7;
        }

        @Override
        public void validarDisponibilidade(BigDecimal pesoTotalKg) {
        }
    },
    EXPRESSA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return new BigDecimal("25.00")
                .add(new BigDecimal("4.50").multiply(pesoTotalKg))
                .setScale(2, java.math.RoundingMode.HALF_EVEN);
        }

        @Override
        public int getPrazoEntregaDias() {
            return 2;
        }

        @Override
        public void validarDisponibilidade(BigDecimal pesoTotalKg) {
        }
    },
    RETIRADA_LOJA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return BigDecimal.ZERO.setScale(2);
        }

        @Override
        public int getPrazoEntregaDias() {
            return 1;
        }

        @Override
        public void validarDisponibilidade(BigDecimal pesoTotalKg) {
        }
    },
    MOTOBOY {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return new BigDecimal("18.00").setScale(2, java.math.RoundingMode.HALF_EVEN);
        }

        @Override
        public int getPrazoEntregaDias() {
            return 0;
        }

        @Override
        public void validarDisponibilidade(BigDecimal pesoTotalKg) {
            if (pesoTotalKg.compareTo(new BigDecimal("5.00")) > 0) {
                throw new CheckoutException("MODALIDADE_INDISPONIVEL");
            }
        }
    };

    public abstract BigDecimal calcularFrete(BigDecimal pesoTotalKg);
    public abstract int getPrazoEntregaDias();
    public abstract void validarDisponibilidade(BigDecimal pesoTotalKg);

    public static ModalidadeEntrega fromString(String valor) {
        if (valor == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        try {
            return ModalidadeEntrega.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
    }
}
