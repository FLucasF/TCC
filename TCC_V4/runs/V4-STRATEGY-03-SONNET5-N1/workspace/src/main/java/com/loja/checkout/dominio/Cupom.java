package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return true;
        }

        @Override
        public BigDecimal desconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return Dinheiro.arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
        }
    },

    MENOS50 {
        @Override
        public boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return subtotalProdutos.compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal desconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return new BigDecimal("50.00");
        }
    },

    FRETEGRATIS {
        @Override
        public boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return true;
        }

        @Override
        public BigDecimal desconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return frete;
        }
    },

    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return true;
        }

        @Override
        public BigDecimal desconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            BigDecimal total = Dinheiro.ZERO;
            for (Item item : itens) {
                int unidadesGratis = item.quantidade() / 3;
                total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return Dinheiro.arredondar(total);
        }
    };

    public abstract boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete);

    public abstract BigDecimal desconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete);
}
