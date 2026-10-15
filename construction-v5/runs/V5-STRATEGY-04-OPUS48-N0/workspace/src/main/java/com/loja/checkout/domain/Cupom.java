package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cupons promocionais. Só vale um por pedido e o código vem sempre em maiúsculas,
 * exatamente como a constante. Ficam fixos no sistema (sem cadastro).
 *
 * <p>Para criar uma promoção nova, adicione uma constante aqui implementando como
 * ela desconta e, se tiver condição, quando é aplicável.
 */
public enum Cupom {

    /** 10% de desconto no valor dos produtos. */
    BEMVINDO10 {
        @Override
        public BigDecimal desconto(Contexto ctx) {
            return Money.percent(ctx.subtotalProdutos(), new BigDecimal("10"));
        }
    },

    /** R$ 50,00 de desconto nos produtos, só a partir de R$ 300,00 em produtos. */
    MENOS50 {
        @Override
        public boolean aplicavel(Contexto ctx) {
            return ctx.subtotalProdutos().compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal desconto(Contexto ctx) {
            return Money.cents(new BigDecimal("50.00"));
        }
    },

    /** Cliente não paga o frete: o desconto fica igual ao valor do frete. */
    FRETEGRATIS {
        @Override
        public BigDecimal desconto(Contexto ctx) {
            return Money.cents(ctx.frete());
        }
    },

    /** A cada 3 unidades de um mesmo item, uma sai de graça. */
    LEVE3PAGUE2 {
        @Override
        public BigDecimal desconto(Contexto ctx) {
            BigDecimal total = BigDecimal.ZERO;
            for (Item item : ctx.itens()) {
                int gratis = item.quantidade() / 3;
                if (gratis > 0) {
                    total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis), Money.MC));
                }
            }
            return Money.cents(total);
        }
    };

    /** Condição do cupom. Por padrão não há condição. */
    public boolean aplicavel(Contexto ctx) {
        return true;
    }

    /** Valor do desconto, em centavos. */
    public abstract BigDecimal desconto(Contexto ctx);

    /** Dados do pedido que os cupons podem precisar para calcular o desconto. */
    public record Contexto(BigDecimal subtotalProdutos, BigDecimal frete, List<Item> itens) {
    }
}
