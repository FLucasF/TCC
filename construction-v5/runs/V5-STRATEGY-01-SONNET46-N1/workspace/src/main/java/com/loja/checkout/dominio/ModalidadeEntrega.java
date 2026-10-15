package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.loja.checkout.dominio.CodigoErro.MODALIDADE_INDISPONIVEL;

public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return new BigDecimal("12.00")
                    .add(new BigDecimal("2.00").multiply(pesoKg))
                    .setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public void validarDisponibilidade(BigDecimal pesoKg) {}
    },

    EXPRESSA(2) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return new BigDecimal("25.00")
                    .add(new BigDecimal("4.50").multiply(pesoKg))
                    .setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public void validarDisponibilidade(BigDecimal pesoKg) {}
    },

    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return BigDecimal.ZERO.setScale(2);
        }

        @Override
        public void validarDisponibilidade(BigDecimal pesoKg) {}
    },

    MOTOBOY(0) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return new BigDecimal("18.00");
        }

        @Override
        public void validarDisponibilidade(BigDecimal pesoKg) {
            if (pesoKg.compareTo(new BigDecimal("5")) > 0)
                throw new CheckoutException(MODALIDADE_INDISPONIVEL);
        }
    };

    public final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    public abstract BigDecimal calcularFrete(BigDecimal pesoKg);

    public abstract void validarDisponibilidade(BigDecimal pesoKg);
}
