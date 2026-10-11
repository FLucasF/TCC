package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Cupons da loja. Cada cupom define o proprio desconto e a propria condicao.
 */
public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal desconto(BaseCupom base) {
            return Dinheiro.percentual(base.subtotalProdutos(), new BigDecimal("0.10"));
        }
    },

    MENOS50 {
        private static final BigDecimal MINIMO_PRODUTOS = new BigDecimal("300.00");

        @Override
        public BigDecimal desconto(BaseCupom base) {
            return Dinheiro.centavos(new BigDecimal("50.00"));
        }

        @Override
        public boolean aplicavel(BaseCupom base) {
            return base.subtotalProdutos().compareTo(MINIMO_PRODUTOS) >= 0;
        }
    },

    FRETEGRATIS {
        @Override
        public BigDecimal desconto(BaseCupom base) {
            return Dinheiro.centavos(base.frete());
        }
    },

    LEVE3PAGUE2 {
        @Override
        public BigDecimal desconto(BaseCupom base) {
            BigDecimal desconto = base.itens().stream()
                    .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return Dinheiro.centavos(desconto);
        }
    };

    public abstract BigDecimal desconto(BaseCupom base);

    public boolean aplicavel(BaseCupom base) {
        return true;
    }
}
