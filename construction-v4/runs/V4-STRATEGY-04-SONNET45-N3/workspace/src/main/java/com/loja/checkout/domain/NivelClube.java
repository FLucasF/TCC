package com.loja.checkout.domain;

import com.loja.checkout.exception.CheckoutException;
import java.math.BigDecimal;

public enum NivelClube {
    BRONZE {
        @Override
        public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
            return BigDecimal.ZERO.setScale(2);
        }

        @Override
        public boolean isentaFrete() {
            return false;
        }

        @Override
        public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },
    PRATA {
        @Override
        public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
            return subtotalProdutos.multiply(new BigDecimal("0.02"))
                .setScale(2, java.math.RoundingMode.HALF_EVEN);
        }

        @Override
        public boolean isentaFrete() {
            return false;
        }

        @Override
        public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },
    OURO {
        @Override
        public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
            return subtotalProdutos.multiply(new BigDecimal("0.05"))
                .setScale(2, java.math.RoundingMode.HALF_EVEN);
        }

        @Override
        public boolean isentaFrete() {
            return true;
        }

        @Override
        public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    public abstract BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    public abstract boolean isentaFrete();
    public abstract boolean ganhaBrinde(BigDecimal subtotalProdutos);

    public static NivelClube fromString(String valor) {
        if (valor == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        try {
            return NivelClube.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
    }
}
