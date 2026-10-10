package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Cupons de desconto. Cada cupom traz a propria condicao e o proprio
 * calculo de desconto; por padrao vale para qualquer pedido.
 */
public enum Cupom {

    BEMVINDO10 {
        private static final BigDecimal TAXA = new BigDecimal("0.10");

        @Override
        public BigDecimal desconto(BaseCupom base) {
            return Dinheiro.percentual(base.subtotalProdutos(), TAXA);
        }
    },

    MENOS50 {
        private static final BigDecimal MINIMO = new BigDecimal("300.00");
        private static final BigDecimal VALOR = new BigDecimal("50.00");

        @Override
        public boolean aplicavel(BaseCupom base) {
            return base.subtotalProdutos().compareTo(MINIMO) >= 0;
        }

        @Override
        public BigDecimal desconto(BaseCupom base) {
            return Dinheiro.centavos(VALOR);
        }
    },

    FRETEGRATIS {
        @Override
        public BigDecimal desconto(BaseCupom base) {
            return Dinheiro.centavos(base.frete());
        }
    },

    LEVE3PAGUE2 {
        private static final int GRUPO = 3;

        @Override
        public BigDecimal desconto(BaseCupom base) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (Item item : base.itens()) {
                BigDecimal gratis = BigDecimal.valueOf(item.quantidade() / GRUPO);
                desconto = desconto.add(item.precoUnitario().multiply(gratis));
            }
            return Dinheiro.centavos(desconto);
        }
    };

    public abstract BigDecimal desconto(BaseCupom base);

    public boolean aplicavel(BaseCupom base) {
        return true;
    }
}
