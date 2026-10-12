package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cupons de desconto. Só se usa um cupom por pedido e o código é sempre em
 * letras maiúsculas, exatamente como o valor do enum.
 *
 * <p>Por enquanto os cupons ficam fixos no sistema. O marketing adora inventar
 * promoção: para criar um cupom novo, adicione um valor aqui implementando
 * {@link #aplicavel} (a condição de uso) e {@link #desconto} (quanto abate dos produtos).
 */
public enum Cupom {

    /** 10% de desconto no valor dos produtos. */
    BEMVINDO10 {
        @Override
        public boolean aplicavel(ContextoCupom ctx) {
            return true;
        }

        @Override
        public BigDecimal desconto(ContextoCupom ctx) {
            return Dinheiro.percentual(ctx.subtotalProdutos(), new BigDecimal("0.10"));
        }
    },

    /** R$ 50,00 de desconto, só para compras a partir de R$ 300,00 em produtos. */
    MENOS50 {
        @Override
        public boolean aplicavel(ContextoCupom ctx) {
            return ctx.subtotalProdutos().compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal desconto(ContextoCupom ctx) {
            return Dinheiro.centavos(new BigDecimal("50.00"));
        }
    },

    /** Frete grátis: o desconto fica igual ao valor do frete mostrado no resumo. */
    FRETEGRATIS {
        @Override
        public boolean aplicavel(ContextoCupom ctx) {
            return true;
        }

        @Override
        public BigDecimal desconto(ContextoCupom ctx) {
            return Dinheiro.centavos(ctx.frete());
        }
    },

    /** A cada 3 unidades de um mesmo item, uma sai de graça. */
    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(ContextoCupom ctx) {
            return true;
        }

        @Override
        public BigDecimal desconto(ContextoCupom ctx) {
            BigDecimal total = BigDecimal.ZERO;
            for (ItemPedido item : ctx.itens()) {
                int gratis = item.quantidade() / 3;
                total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
            }
            return Dinheiro.centavos(total);
        }
    };

    /** Indica se o pedido cumpre a condição de uso do cupom. */
    public abstract boolean aplicavel(ContextoCupom ctx);

    /** Valor que o cupom abate do valor dos produtos, arredondado para centavos. */
    public abstract BigDecimal desconto(ContextoCupom ctx);

    /**
     * Dados do pedido de que os cupons precisam.
     *
     * @param subtotalProdutos soma dos produtos
     * @param frete            frete mostrado no resumo (já com eventual frete grátis do clube)
     * @param itens            itens do carrinho
     */
    public record ContextoCupom(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemPedido> itens) {
    }
}
