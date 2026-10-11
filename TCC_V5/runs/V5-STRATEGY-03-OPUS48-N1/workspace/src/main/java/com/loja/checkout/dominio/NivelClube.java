package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Niveis do clube da loja. Cada nivel tem o proprio conjunto de vantagens
 * (credito de volta, isencao de frete, brinde), entao cada caso mora no
 * proprio corpo do enum. Nivel novo entra como uma nova constante.
 */
public enum NivelClube {

    BRONZE("0.00", false) {
        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    PRATA("0.02", false) {
        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    OURO("0.05", true) {
        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    private final BigDecimal percentualCredito;
    private final boolean isentaFrete;

    NivelClube(String percentualCredito, boolean isentaFrete) {
        this.percentualCredito = new BigDecimal(percentualCredito);
        this.isentaFrete = isentaFrete;
    }

    /** Credito bruto para a proxima compra; quem chama arredonda para centavos. */
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(percentualCredito);
    }

    public boolean isentaFrete() {
        return isentaFrete;
    }

    /** Se o pedido ganha brinde. */
    public abstract boolean temBrinde(BigDecimal subtotalProdutos);
}
