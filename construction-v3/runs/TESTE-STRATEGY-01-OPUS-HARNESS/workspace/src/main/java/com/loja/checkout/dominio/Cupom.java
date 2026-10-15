package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Cada cupom guarda no seu proprio corpo sua condicao e sua conta de desconto. */
public enum Cupom {

    BEMVINDO10 {
        @Override
        protected BigDecimal desconto(BaseCupom base) {
            return Dinheiro.percentual(base.subtotalProdutos(), new BigDecimal("0.10"));
        }
    },

    MENOS50 {
        private static final BigDecimal MINIMO = new BigDecimal("300.00");

        @Override
        public boolean aplicavel(BaseCupom base) {
            return base.subtotalProdutos().compareTo(MINIMO) >= 0;
        }

        @Override
        protected BigDecimal desconto(BaseCupom base) {
            return new BigDecimal("50.00");
        }
    },

    FRETEGRATIS {
        @Override
        protected BigDecimal desconto(BaseCupom base) {
            return base.frete();
        }
    },

    LEVE3PAGUE2 {
        @Override
        protected BigDecimal desconto(BaseCupom base) {
            return base.carrinho().itens().stream()
                    .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    };

    protected abstract BigDecimal desconto(BaseCupom base);

    public boolean aplicavel(BaseCupom base) {
        return true;
    }

    public BigDecimal abatimento(BaseCupom base) {
        return Dinheiro.centavos(desconto(base));
    }
}
