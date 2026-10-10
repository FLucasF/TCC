package com.loja.checkout.clube;

import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;
import java.util.Optional;

public enum NivelClube {

    BRONZE {
        @Override
        public BigDecimal calcularCredito(BigDecimal subtotal) {
            return new BigDecimal("0.00");
        }

        @Override
        public boolean isentaFrete() {
            return false;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotal) {
            return false;
        }
    },

    PRATA {
        @Override
        public BigDecimal calcularCredito(BigDecimal subtotal) {
            return Dinheiro.arredondar(subtotal.multiply(new BigDecimal("0.02")));
        }

        @Override
        public boolean isentaFrete() {
            return false;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotal) {
            return false;
        }
    },

    OURO {
        @Override
        public BigDecimal calcularCredito(BigDecimal subtotal) {
            return Dinheiro.arredondar(subtotal.multiply(new BigDecimal("0.05")));
        }

        @Override
        public boolean isentaFrete() {
            return true;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotal) {
            return subtotal.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    public abstract BigDecimal calcularCredito(BigDecimal subtotal);

    public abstract boolean isentaFrete();

    public abstract boolean temBrinde(BigDecimal subtotal);

    public static Optional<NivelClube> fromCodigo(String codigo) {
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
