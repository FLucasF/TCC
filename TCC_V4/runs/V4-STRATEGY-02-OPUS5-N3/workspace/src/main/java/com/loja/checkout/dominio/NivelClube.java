package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Nível do cliente no clube da loja. As vantagens de cada nível ficam no corpo
 * da própria constante; níveis novos entram como constantes novas.
 */
public enum NivelClube {

    BRONZE("0"),

    PRATA("0.02"),

    OURO("0.05") {
        private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

        @Override
        public BigDecimal freteDevido(BigDecimal frete) {
            return Dinheiro.ZERO;
        }

        @Override
        public boolean brinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
        }
    };

    private final BigDecimal percentualCredito;

    NivelClube(String percentualCredito) {
        this.percentualCredito = new BigDecimal(percentualCredito);
    }

    /** Crédito para a próxima compra, sobre o valor dos produtos. */
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, percentualCredito);
    }

    /** Quanto do frete o cliente deste nível realmente paga. */
    public BigDecimal freteDevido(BigDecimal frete) {
        return frete;
    }

    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
