package com.loja.checkout.domain.entrega;

import com.loja.checkout.exception.CheckoutException;

import java.math.BigDecimal;

import static com.loja.checkout.util.Moeda.arredondar;

public enum ModalidadeEntrega {
    ECONOMICA {
        @Override
        public boolean aceitaPedido(BigDecimal pesoTotalKg) {
            return true;
        }

        @Override
        public ResultadoEntrega calcular(BigDecimal pesoTotalKg) {
            BigDecimal frete = arredondar(
                new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoTotalKg))
            );
            return new ResultadoEntrega(frete, 7);
        }
    },
    EXPRESSA {
        @Override
        public boolean aceitaPedido(BigDecimal pesoTotalKg) {
            return true;
        }

        @Override
        public ResultadoEntrega calcular(BigDecimal pesoTotalKg) {
            BigDecimal frete = arredondar(
                new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoTotalKg))
            );
            return new ResultadoEntrega(frete, 2);
        }
    },
    RETIRADA_LOJA {
        @Override
        public boolean aceitaPedido(BigDecimal pesoTotalKg) {
            return true;
        }

        @Override
        public ResultadoEntrega calcular(BigDecimal pesoTotalKg) {
            return new ResultadoEntrega(BigDecimal.ZERO.setScale(2), 1);
        }
    },
    MOTOBOY {
        @Override
        public boolean aceitaPedido(BigDecimal pesoTotalKg) {
            return pesoTotalKg.compareTo(new BigDecimal("5")) <= 0;
        }

        @Override
        public ResultadoEntrega calcular(BigDecimal pesoTotalKg) {
            if (!aceitaPedido(pesoTotalKg)) {
                throw new CheckoutException("MODALIDADE_INDISPONIVEL");
            }
            return new ResultadoEntrega(new BigDecimal("18.00"), 0);
        }
    };

    public abstract boolean aceitaPedido(BigDecimal pesoTotalKg);
    public abstract ResultadoEntrega calcular(BigDecimal pesoTotalKg);
}
