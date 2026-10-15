package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Cada nivel do clube carrega o proprio conjunto de vantagens:
 * credito para a proxima compra, frete gratis e brinde.
 */
public enum NivelClube {

    BRONZE,

    PRATA {
        @Override
        public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
            return percentual(subtotalProdutos, "0.02");
        }
    },

    OURO {
        @Override
        public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
            return percentual(subtotalProdutos, "0.05");
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

    public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    public boolean freteGratis() {
        return false;
    }

    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }

    static BigDecimal percentual(BigDecimal valor, String taxa) {
        return Dinheiro.centavos(valor.multiply(new BigDecimal(taxa)));
    }
}
