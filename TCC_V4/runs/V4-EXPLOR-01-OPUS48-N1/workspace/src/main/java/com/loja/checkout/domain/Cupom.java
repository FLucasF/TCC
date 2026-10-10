package com.loja.checkout.domain;

import static com.loja.checkout.domain.Dinheiro.centavos;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cupons. O que cada cupom desconta e quando ele se aplica varia de um para
 * outro, então cada caso tem o seu próprio corpo. O contexto reúne tudo que
 * algum cupom possa precisar (subtotal, frete e os itens), para a assinatura
 * atender o caso mais exigente.
 */
public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal desconto(Contexto ctx) {
            return centavos(ctx.subtotalProdutos().multiply(new BigDecimal("0.10")));
        }
    },

    MENOS50 {
        @Override
        public boolean aplicavel(Contexto ctx) {
            return ctx.subtotalProdutos().compareTo(BigDecimal.valueOf(300)) >= 0;
        }

        @Override
        public BigDecimal desconto(Contexto ctx) {
            return centavos(BigDecimal.valueOf(50));
        }
    },

    FRETEGRATIS {
        @Override
        public BigDecimal desconto(Contexto ctx) {
            return centavos(ctx.frete());
        }
    },

    LEVE3PAGUE2 {
        @Override
        public BigDecimal desconto(Contexto ctx) {
            BigDecimal gratis = ctx.itens().stream()
                    .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return centavos(gratis);
        }
    };

    public boolean aplicavel(Contexto ctx) {
        return true;
    }

    public abstract BigDecimal desconto(Contexto ctx);

    /** Tudo que algum cupom possa precisar para calcular. */
    public record Contexto(BigDecimal subtotalProdutos, BigDecimal frete, List<Item> itens) {
    }
}
