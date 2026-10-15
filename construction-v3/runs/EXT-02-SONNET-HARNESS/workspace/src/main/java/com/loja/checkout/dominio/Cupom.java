package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos) {
            return true;
        }

        @Override
        public BigDecimal desconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return Dinheiro.arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
        }
    },

    MENOS50 {
        private static final BigDecimal VALOR_MINIMO_PRODUTOS = new BigDecimal("300.00");

        @Override
        public boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(VALOR_MINIMO_PRODUTOS) >= 0;
        }

        @Override
        public BigDecimal desconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return Dinheiro.arredondar(new BigDecimal("50.00"));
        }
    },

    FRETEGRATIS {
        @Override
        public boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos) {
            return true;
        }

        @Override
        public BigDecimal desconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return Dinheiro.arredondar(frete);
        }
    },

    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos) {
            return true;
        }

        @Override
        public BigDecimal desconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            BigDecimal total = BigDecimal.ZERO;
            for (Item item : itens) {
                int unidadesGratis = item.quantidade() / 3;
                total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return Dinheiro.arredondar(total);
        }
    };

    public abstract boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos);

    public abstract BigDecimal desconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete);
}
