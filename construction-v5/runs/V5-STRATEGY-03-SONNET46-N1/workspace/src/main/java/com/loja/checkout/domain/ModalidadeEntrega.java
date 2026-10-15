package com.loja.checkout.domain;

import java.math.BigDecimal;

import static com.loja.checkout.domain.Arredondamento.*;

public enum ModalidadeEntrega {

    ECONOMICA {
        @Override public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return r(new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg)));
        }
        @Override public int prazoEntregaDias() { return 7; }
        @Override public void validarDisponibilidade(BigDecimal pesoKg) {}
    },

    EXPRESSA {
        @Override public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return r(new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg)));
        }
        @Override public int prazoEntregaDias() { return 2; }
        @Override public void validarDisponibilidade(BigDecimal pesoKg) {}
    },

    RETIRADA_LOJA {
        @Override public BigDecimal calcularFrete(BigDecimal pesoKg) { return ZERO; }
        @Override public int prazoEntregaDias() { return 1; }
        @Override public void validarDisponibilidade(BigDecimal pesoKg) {}
    },

    MOTOBOY {
        @Override public BigDecimal calcularFrete(BigDecimal pesoKg) { return new BigDecimal("18.00"); }
        @Override public int prazoEntregaDias() { return 0; }
        @Override public void validarDisponibilidade(BigDecimal pesoKg) {
            if (pesoKg.compareTo(new BigDecimal("5")) > 0)
                throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    };

    public abstract BigDecimal calcularFrete(BigDecimal pesoKg);

    public abstract int prazoEntregaDias();

    public abstract void validarDisponibilidade(BigDecimal pesoKg);
}
