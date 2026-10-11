package com.loja.resumo.domain;

import java.math.BigDecimal;

/**
 * Cada nível do clube tem seu próprio conjunto de vantagens, calculado de uma
 * vez em calcularBeneficio. Novos níveis só precisam de uma nova constante.
 */
public enum NivelClube {

    BRONZE {
        @Override
        public BeneficioClube calcularBeneficio(BigDecimal subtotalProdutos) {
            return new BeneficioClube(BigDecimal.ZERO.setScale(2), false, false);
        }
    },

    PRATA {
        @Override
        public BeneficioClube calcularBeneficio(BigDecimal subtotalProdutos) {
            BigDecimal credito = Dinheiro.arredondar(subtotalProdutos.multiply(new BigDecimal("0.02")));
            return new BeneficioClube(credito, false, false);
        }
    },

    OURO {
        @Override
        public BeneficioClube calcularBeneficio(BigDecimal subtotalProdutos) {
            BigDecimal credito = Dinheiro.arredondar(subtotalProdutos.multiply(new BigDecimal("0.05")));
            boolean brinde = subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
            return new BeneficioClube(credito, true, brinde);
        }
    };

    public abstract BeneficioClube calcularBeneficio(BigDecimal subtotalProdutos);
}
