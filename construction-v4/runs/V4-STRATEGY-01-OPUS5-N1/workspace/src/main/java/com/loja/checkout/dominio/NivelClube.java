package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Niveis do clube da loja. O credito para a proxima compra tem a mesma
 * conta em todos os niveis (percentual sobre os produtos), so a taxa muda;
 * as demais vantagens ficam em cada nivel.
 */
public enum NivelClube {

    BRONZE("0.00"),

    PRATA("0.02"),

    OURO("0.05") {
        private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

        @Override
        public boolean freteGratis() {
            return true;
        }

        @Override
        public boolean brinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
        }
    };

    private final BigDecimal taxaCredito;

    NivelClube(String taxaCredito) {
        this.taxaCredito = new BigDecimal(taxaCredito);
    }

    public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, taxaCredito);
    }

    public boolean freteGratis() {
        return false;
    }

    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
