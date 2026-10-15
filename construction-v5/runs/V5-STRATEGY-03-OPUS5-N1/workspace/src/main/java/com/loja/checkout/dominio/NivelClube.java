package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Optional;

/** Nível do cliente no clube da loja, com as vantagens de cada um. */
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
            return Dinheiro.percentual(subtotalProdutos, new BigDecimal("2"));
        }
    },

    OURO {
        private static final BigDecimal SUBTOTAL_PARA_BRINDE = new BigDecimal("500.00");

        @Override
        public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
            return Dinheiro.percentual(subtotalProdutos, new BigDecimal("5"));
        }

        @Override
        public boolean isentaFrete() {
            return true;
        }

        @Override
        public boolean brinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(SUBTOTAL_PARA_BRINDE) > 0;
        }
    };

    private static final Catalogo<NivelClube> CATALOGO = Catalogo.de(NivelClube.class);

    public static Optional<NivelClube> de(String codigo) {
        return CATALOGO.buscar(codigo);
    }

    public abstract BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos);

    public boolean isentaFrete() {
        return false;
    }

    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
