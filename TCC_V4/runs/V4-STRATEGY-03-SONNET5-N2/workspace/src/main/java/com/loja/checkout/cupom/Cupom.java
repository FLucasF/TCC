package com.loja.checkout.cupom;

import com.loja.checkout.pedido.ItemPedido;

import java.math.BigDecimal;
import java.util.List;

import static java.math.RoundingMode.HALF_EVEN;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public boolean aplicavel(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return subtotalProdutos.multiply(new BigDecimal("0.10")).setScale(2, HALF_EVEN);
        }
    },

    MENOS50 {
        @Override
        public boolean aplicavel(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return subtotalProdutos.compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return new BigDecimal("50.00");
        }
    },

    FRETEGRATIS {
        @Override
        public boolean aplicavel(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return frete;
        }
    },

    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemPedido item : itens) {
                int unidadesGratis = item.quantidade() / 3;
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return desconto.setScale(2, HALF_EVEN);
        }
    };

    public abstract boolean aplicavel(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete);

    public abstract BigDecimal calcularDesconto(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete);
}
