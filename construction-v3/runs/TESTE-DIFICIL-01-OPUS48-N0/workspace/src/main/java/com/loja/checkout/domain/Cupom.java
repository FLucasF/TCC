package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cupons de desconto que valem hoje. So vale um cupom por pedido e o codigo e
 * sempre em maiusculas, exatamente como na constante. Cada cupom sabe dizer se
 * se aplica ao pedido e qual o valor do desconto (ja arredondado para centavos).
 * Para criar uma promocao nova, basta adicionar uma constante aqui.
 */
public enum Cupom {

    /** 10% de desconto no valor dos produtos. */
    BEMVINDO10 {
        @Override
        public BigDecimal desconto(Contexto ctx) {
            return Dinheiro.centavos(ctx.subtotalProdutos().multiply(new BigDecimal("0.10")));
        }
    },

    /** R$ 50,00 de desconto, so para produtos a partir de R$ 300,00. */
    MENOS50 {
        @Override
        public boolean aplicavel(Contexto ctx) {
            return ctx.subtotalProdutos().compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal desconto(Contexto ctx) {
            return Dinheiro.centavos(new BigDecimal("50.00"));
        }
    },

    /** O cliente nao paga o frete: o desconto fica igual ao valor do frete. */
    FRETEGRATIS {
        @Override
        public BigDecimal desconto(Contexto ctx) {
            return Dinheiro.centavos(ctx.frete());
        }
    },

    /** A cada 3 unidades de um mesmo item, uma sai de graca. */
    LEVE3PAGUE2 {
        @Override
        public BigDecimal desconto(Contexto ctx) {
            BigDecimal total = BigDecimal.ZERO;
            for (ItemPedido item : ctx.itens()) {
                int gratis = item.quantidade() / 3;
                total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
            }
            return Dinheiro.centavos(total);
        }
    };

    /** True quando o pedido cumpre a condicao do cupom. */
    public boolean aplicavel(Contexto ctx) {
        return true;
    }

    /** Valor do desconto do cupom, arredondado para centavos. */
    public abstract BigDecimal desconto(Contexto ctx);

    /**
     * Dados do pedido que os cupons podem precisar: o valor dos produtos, o
     * frete (ja arredondado) e os itens do carrinho.
     */
    public record Contexto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemPedido> itens) {
    }
}
