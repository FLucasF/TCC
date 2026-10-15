package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cupons de desconto. So vale um cupom por pedido e o codigo e sempre em
 * letras maiusculas, exatamente como esta aqui. Os cupons ficam fixos no
 * sistema (nao ha cadastro).
 *
 * Cada cupom sabe dizer se e aplicavel ao pedido e quanto desconta.
 */
public enum Cupom {

    /** 10% de desconto no valor dos produtos. */
    BEMVINDO10 {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete) {
            return Money.cents(subtotalProdutos.multiply(new BigDecimal("0.10")));
        }
    },

    /** R$ 50,00 de desconto, so para compras a partir de R$ 300,00 em produtos. */
    MENOS50 {
        private final BigDecimal minimo = new BigDecimal("300.00");

        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(minimo) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete) {
            return Money.cents(new BigDecimal("50.00"));
        }
    },

    /** O cliente nao paga o frete: o desconto fica igual ao valor do frete. */
    FRETEGRATIS {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete) {
            return Money.cents(frete);
        }
    },

    /** A cada 3 unidades de um mesmo item, uma sai de graca. */
    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemPedido item : itens) {
                int gratis = item.quantidade() / 3;
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
            }
            return Money.cents(desconto);
        }
    };

    /** Diz se o cupom pode ser usado neste pedido. */
    public abstract boolean aplicavel(BigDecimal subtotalProdutos);

    /** Calcula o valor do desconto do cupom, arredondado para centavos. */
    public abstract BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete);

    public static Cupom fromCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (Cupom c : values()) {
            if (c.name().equals(codigo)) {
                return c;
            }
        }
        return null;
    }
}
