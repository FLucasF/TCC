package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/**
 * Cada nível do clube tem seu conjunto de vantagens: crédito de volta, frete
 * grátis e brinde. Nível novo = nova constante aqui.
 */
public enum NivelClube {

    BRONZE {
        @Override
        public BigDecimal credito(BigDecimal subtotalProdutos) {
            return Dinheiro.ZERO;
        }
    },
    PRATA {
        @Override
        public BigDecimal credito(BigDecimal subtotalProdutos) {
            return Dinheiro.arredonda(subtotalProdutos.multiply(new BigDecimal("0.02")));
        }
    },
    OURO {
        @Override
        public BigDecimal credito(BigDecimal subtotalProdutos) {
            return Dinheiro.arredonda(subtotalProdutos.multiply(new BigDecimal("0.05")));
        }

        @Override
        public boolean freteGratis() {
            return true;
        }

        @Override
        public boolean brinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    public abstract BigDecimal credito(BigDecimal subtotalProdutos);

    public boolean freteGratis() {
        return false;
    }

    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }

    public static Optional<NivelClube> fromCodigo(String codigo) {
        return Arrays.stream(values()).filter(n -> n.name().equals(codigo)).findFirst();
    }
}
