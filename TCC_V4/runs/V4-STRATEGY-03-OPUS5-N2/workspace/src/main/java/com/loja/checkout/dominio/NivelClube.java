package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Niveis do clube da loja. Cada nivel tem seu conjunto de vantagens (credito
 * para a proxima compra, isencao de frete e brinde), e cada conjunto mora na
 * sua propria constante: criar um nivel novo e acrescentar uma constante aqui.
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
            return Dinheiro.percentual(subtotalProdutos, new BigDecimal("0.02"));
        }
    },

    OURO {
        private static final BigDecimal PRODUTOS_PARA_BRINDE = new BigDecimal("500.00");

        @Override
        public BigDecimal credito(BigDecimal subtotalProdutos) {
            return Dinheiro.percentual(subtotalProdutos, new BigDecimal("0.05"));
        }

        @Override
        public BigDecimal freteACobrar(BigDecimal freteDaModalidade) {
            return Dinheiro.ZERO;
        }

        @Override
        public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(PRODUTOS_PARA_BRINDE) > 0;
        }
    };

    /** Credito para a proxima compra, sobre o valor dos produtos. */
    public abstract BigDecimal credito(BigDecimal subtotalProdutos);

    /** Por padrao o nivel nao mexe no frete cotado pela modalidade. */
    public BigDecimal freteACobrar(BigDecimal freteDaModalidade) {
        return freteDaModalidade;
    }

    /** Por padrao o nivel nao da brinde. */
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
