package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, List<Item> itens) {
            return true;
        }

        @Override
        public BigDecimal desconto(BigDecimal subtotalProdutos, List<Item> itens, BigDecimal frete) {
            return Dinheiro.arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
        }
    },

    MENOS50 {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, List<Item> itens) {
            return subtotalProdutos.compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal desconto(BigDecimal subtotalProdutos, List<Item> itens, BigDecimal frete) {
            return new BigDecimal("50.00");
        }
    },

    FRETEGRATIS {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, List<Item> itens) {
            return true;
        }

        @Override
        public BigDecimal desconto(BigDecimal subtotalProdutos, List<Item> itens, BigDecimal frete) {
            return frete;
        }
    },

    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, List<Item> itens) {
            return true;
        }

        @Override
        public BigDecimal desconto(BigDecimal subtotalProdutos, List<Item> itens, BigDecimal frete) {
            BigDecimal total = BigDecimal.ZERO;
            for (Item item : itens) {
                int unidadesGratis = item.quantidade() / 3;
                total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return Dinheiro.arredondar(total);
        }
    };

    public abstract boolean aplicavel(BigDecimal subtotalProdutos, List<Item> itens);

    public abstract BigDecimal desconto(BigDecimal subtotalProdutos, List<Item> itens, BigDecimal frete);
}
