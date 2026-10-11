package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.Dinheiro;
import com.loja.checkout.domain.Item;

import java.math.BigDecimal;
import java.util.List;

public enum Cupom {

    BEMVINDO10 {
        @Override public boolean aplicavel(ContextoCupom ctx) { return true; }
        @Override public BigDecimal desconto(ContextoCupom ctx) {
            return Dinheiro.ajustar(ctx.subtotalProdutos().multiply(new BigDecimal("0.10")));
        }
    },

    MENOS50 {
        private final BigDecimal minimo = new BigDecimal("300");
        @Override public boolean aplicavel(ContextoCupom ctx) {
            return ctx.subtotalProdutos().compareTo(minimo) >= 0;
        }
        @Override public BigDecimal desconto(ContextoCupom ctx) {
            return Dinheiro.ajustar(new BigDecimal("50"));
        }
    },

    FRETEGRATIS {
        @Override public boolean aplicavel(ContextoCupom ctx) { return true; }
        @Override public BigDecimal desconto(ContextoCupom ctx) {
            return Dinheiro.ajustar(ctx.frete());
        }
    },

    LEVE3PAGUE2 {
        @Override public boolean aplicavel(ContextoCupom ctx) { return true; }
        @Override public BigDecimal desconto(ContextoCupom ctx) {
            BigDecimal total = BigDecimal.ZERO;
            for (Item item : ctx.itens()) {
                int gratis = item.quantidade() / 3;
                if (gratis > 0) {
                    total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
                }
            }
            return Dinheiro.ajustar(total);
        }
    };

    public abstract boolean aplicavel(ContextoCupom ctx);
    public abstract BigDecimal desconto(ContextoCupom ctx);

    public record ContextoCupom(BigDecimal subtotalProdutos, BigDecimal frete, List<Item> itens) {}
}
