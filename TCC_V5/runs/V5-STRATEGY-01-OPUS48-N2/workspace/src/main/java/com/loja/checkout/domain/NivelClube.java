package com.loja.checkout.domain;

import com.loja.checkout.dinheiro.Dinheiro;

import java.math.BigDecimal;

/**
 * Níveis do clube. Cada nível tem o seu conjunto de vantagens: percentual de
 * crédito, se paga frete e se manda brinde. O caso-base é "não ganha nada"
 * (BRONZE); cada nível sobrescreve só o que o diferencia. Nível novo é uma
 * constante nova com as suas vantagens.
 */
public enum NivelClube {

    BRONZE {
        @Override
        protected BigDecimal percentualCredito() {
            return BigDecimal.ZERO;
        }
    },

    PRATA {
        @Override
        protected BigDecimal percentualCredito() {
            return new BigDecimal("0.02");
        }
    },

    OURO {
        @Override
        protected BigDecimal percentualCredito() {
            return new BigDecimal("0.05");
        }

        @Override
        public BigDecimal frete(BigDecimal freteBase) {
            return Dinheiro.arredondar(BigDecimal.ZERO);
        }

        @Override
        public boolean brinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("500")) > 0;
        }
    };

    /** Percentual do crédito sobre os produtos; é só o dado que muda. */
    protected abstract BigDecimal percentualCredito();

    /** Crédito para a próxima compra, sobre os produtos, já em centavos. */
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(percentualCredito().multiply(subtotalProdutos));
    }

    /** Frete que o cliente paga; por padrão paga o da modalidade. */
    public BigDecimal frete(BigDecimal freteBase) {
        return freteBase;
    }

    /** Se o pedido leva brinde. */
    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
