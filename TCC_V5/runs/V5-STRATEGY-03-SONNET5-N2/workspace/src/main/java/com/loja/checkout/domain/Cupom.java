package com.loja.checkout.domain;

import com.loja.checkout.service.CheckoutException;

import java.math.BigDecimal;
import java.util.List;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, List<Item> itens) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Item> itens, BigDecimal freteExibido) {
            return subtotalProdutos.multiply(new BigDecimal("0.10"));
        }
    },

    MENOS50 {
        private static final BigDecimal MINIMO = new BigDecimal("300.00");

        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, List<Item> itens) {
            return subtotalProdutos.compareTo(MINIMO) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Item> itens, BigDecimal freteExibido) {
            return new BigDecimal("50.00");
        }
    },

    FRETEGRATIS {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, List<Item> itens) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Item> itens, BigDecimal freteExibido) {
            return freteExibido;
        }
    },

    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos, List<Item> itens) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Item> itens, BigDecimal freteExibido) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (Item item : itens) {
                int unidadesGratis = item.quantidade() / 3;
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return desconto;
        }
    };

    public abstract boolean aplicavel(BigDecimal subtotalProdutos, List<Item> itens);

    public abstract BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<Item> itens, BigDecimal freteExibido);

    public static Cupom from(String codigo) {
        try {
            return valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("CUPOM_INVALIDO");
        }
    }
}
