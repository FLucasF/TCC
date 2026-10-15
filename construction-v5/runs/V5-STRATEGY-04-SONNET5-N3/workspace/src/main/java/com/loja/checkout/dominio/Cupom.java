package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemPedido> itens) {
            return true;
        }

        @Override
        public BigDecimal desconto(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete) {
            return subtotalProdutos.multiply(new BigDecimal("0.10"));
        }
    },

    MENOS50 {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemPedido> itens) {
            return subtotalProdutos.compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal desconto(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete) {
            return new BigDecimal("50.00");
        }
    },

    FRETEGRATIS {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemPedido> itens) {
            return true;
        }

        @Override
        public BigDecimal desconto(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete) {
            return frete;
        }
    },

    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemPedido> itens) {
            return true;
        }

        @Override
        public BigDecimal desconto(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete) {
            BigDecimal total = BigDecimal.ZERO;
            for (ItemPedido item : itens) {
                int unidadesGratis = item.quantidade() / 3;
                total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return total;
        }
    };

    public abstract boolean aplicavel(BigDecimal subtotalProdutos, List<ItemPedido> itens);

    public abstract BigDecimal desconto(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete);
}
