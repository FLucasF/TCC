package br.com.loja.checkout;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE {
        @Override
        public BigDecimal credito(BigDecimal subtotalProdutos) {
            return Dinheiro.centavos(BigDecimal.ZERO);
        }
    },

    PRATA {
        @Override
        public BigDecimal credito(BigDecimal subtotalProdutos) {
            return Dinheiro.percentual(subtotalProdutos, "2");
        }
    },

    OURO {
        private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

        @Override
        public BigDecimal credito(BigDecimal subtotalProdutos) {
            return Dinheiro.percentual(subtotalProdutos, "5");
        }

        @Override
        public boolean isentaFrete() {
            return true;
        }

        @Override
        public boolean brinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(LIMITE_BRINDE) > 0;
        }
    };

    /** Crédito para a próxima compra; não abate nada nesta. */
    public abstract BigDecimal credito(BigDecimal subtotalProdutos);

    public boolean isentaFrete() {
        return false;
    }

    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
