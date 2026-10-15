package com.loja.checkout.dominio;

import com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;

/**
 * Níveis do clube da loja. Cada nível reúne as suas vantagens: crédito para a próxima
 * compra, isenção de frete e brinde. Nível novo = constante nova aqui.
 */
public enum NivelClube {

    BRONZE {
        @Override
        public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
            return Dinheiro.ZERO;
        }
    },

    PRATA {
        @Override
        public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
            return Dinheiro.percentual(subtotalProdutos, new BigDecimal("0.02"));
        }
    },

    OURO {
        private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

        @Override
        public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
            return Dinheiro.percentual(subtotalProdutos, new BigDecimal("0.05"));
        }

        @Override
        public BigDecimal frete(BigDecimal freteDaModalidade) {
            return Dinheiro.ZERO;
        }

        @Override
        public boolean brinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
        }
    };

    public abstract BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos);

    /** Quanto de frete este nível paga. */
    public BigDecimal frete(BigDecimal freteDaModalidade) {
        return freteDaModalidade;
    }

    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
