package com.loja.checkout.domain;

import com.loja.checkout.dto.ItemDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public enum Cupom {
    BEMVINDO10 {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemDto> itens) {
            return subtotalProdutos.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public boolean isAplicavel(BigDecimal subtotalProdutos) { return true; }
    },

    MENOS50 {
        private static final BigDecimal MINIMO = new BigDecimal("300.00");

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemDto> itens) {
            return new BigDecimal("50.00");
        }

        @Override
        public boolean isAplicavel(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(MINIMO) >= 0;
        }
    },

    FRETEGRATIS {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemDto> itens) {
            return frete;
        }

        @Override
        public boolean isAplicavel(BigDecimal subtotalProdutos) { return true; }
    },

    LEVE3PAGUE2 {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemDto> itens) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemDto item : itens) {
                int gratis = item.quantidade() / 3;
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
            }
            return desconto.setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public boolean isAplicavel(BigDecimal subtotalProdutos) { return true; }
    };

    public abstract BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemDto> itens);

    public abstract boolean isAplicavel(BigDecimal subtotalProdutos);
}
