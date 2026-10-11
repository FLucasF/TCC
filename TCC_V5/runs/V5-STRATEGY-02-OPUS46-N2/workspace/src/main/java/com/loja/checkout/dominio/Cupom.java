package com.loja.checkout.dominio;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotalProdutos) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return subtotalProdutos.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_EVEN);
        }
    },

    MENOS50 {
        @Override
        public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return new BigDecimal("50.00");
        }
    },

    FRETEGRATIS {
        @Override
        public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotalProdutos) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            return frete;
        }
    },

    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotalProdutos) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemRequest item : itens) {
                int gratis = item.quantidade() / 3;
                desconto = desconto.add(item.precoUnitario().multiply(new BigDecimal(gratis)));
            }
            return desconto.setScale(2, RoundingMode.HALF_EVEN);
        }
    };

    public abstract boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotalProdutos);

    public abstract BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete);
}
