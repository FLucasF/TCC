package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/**
 * Nível do cliente no clube da loja. Cada nível tem o seu conjunto de vantagens no seu
 * próprio corpo: um nível novo é uma constante nova com as vantagens dele.
 */
public enum NivelClube {

    /** Só o cadastro, não ganha nada. */
    BRONZE {
        @Override
        public BigDecimal credito(BigDecimal subtotalProdutos) {
            return Dinheiro.ZERO;
        }
    },

    /** 2% dos produtos de volta em crédito. */
    PRATA {
        @Override
        public BigDecimal credito(BigDecimal subtotalProdutos) {
            return Dinheiro.percentual(subtotalProdutos, new BigDecimal("0.02"));
        }
    },

    /** 5% dos produtos em crédito, não paga frete e ganha brinde acima de R$ 500,00. */
    OURO {
        @Override
        public BigDecimal credito(BigDecimal subtotalProdutos) {
            return Dinheiro.percentual(subtotalProdutos, new BigDecimal("0.05"));
        }

        @Override
        public BigDecimal freteCobrado(BigDecimal frete) {
            return Dinheiro.ZERO;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    /** Crédito guardado para a próxima compra. */
    public abstract BigDecimal credito(BigDecimal subtotalProdutos);

    /** Quanto deste frete o cliente paga. */
    public BigDecimal freteCobrado(BigDecimal frete) {
        return frete;
    }

    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
