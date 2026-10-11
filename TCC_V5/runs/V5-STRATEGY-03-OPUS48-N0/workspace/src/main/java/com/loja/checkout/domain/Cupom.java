package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cupons de desconto da loja. So vale um por pedido.
 *
 * <p>Cada cupom traz sua propria regra: {@link #aplicavel} diz se o pedido cumpre a
 * condicao do cupom, e {@link #descontoBruto} calcula o desconto (ainda sem arredondar;
 * quem arredonda para centavos e o servico). Para criar uma promocao nova, basta
 * acrescentar um valor aqui com as suas duas regras.</p>
 */
public enum Cupom {

    /** 10% de desconto no valor dos produtos. */
    BEMVINDO10 {
        @Override
        public boolean aplicavel(Contexto ctx) {
            return true;
        }

        @Override
        public BigDecimal descontoBruto(Contexto ctx) {
            return ctx.subtotalProdutos().multiply(new BigDecimal("0.10"));
        }
    },

    /** R$ 50,00 de desconto, so para compras a partir de R$ 300,00 em produtos. */
    MENOS50 {
        @Override
        public boolean aplicavel(Contexto ctx) {
            return ctx.subtotalProdutos().compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal descontoBruto(Contexto ctx) {
            return new BigDecimal("50.00");
        }
    },

    /** Frete gratis: o desconto fica igual ao valor do frete. */
    FRETEGRATIS {
        @Override
        public boolean aplicavel(Contexto ctx) {
            return true;
        }

        @Override
        public BigDecimal descontoBruto(Contexto ctx) {
            return ctx.frete();
        }
    },

    /** A cada 3 unidades de um mesmo item, 1 sai de graca. */
    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(Contexto ctx) {
            return true;
        }

        @Override
        public BigDecimal descontoBruto(Contexto ctx) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (LinhaCarrinho linha : ctx.itens()) {
                int gratis = linha.quantidade() / 3;
                desconto = desconto.add(linha.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
            }
            return desconto;
        }
    };

    /** Diz se o pedido cumpre a condicao do cupom. */
    public abstract boolean aplicavel(Contexto ctx);

    /** Desconto do cupom, ainda sem arredondar. */
    public abstract BigDecimal descontoBruto(Contexto ctx);

    /**
     * Dados do pedido de que os cupons precisam.
     *
     * @param itens             linhas do carrinho
     * @param subtotalProdutos  soma dos produtos (ja arredondada)
     * @param frete             frete do pedido (ja arredondado, ja considerando o clube)
     */
    public record Contexto(List<LinhaCarrinho> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
    }
}
