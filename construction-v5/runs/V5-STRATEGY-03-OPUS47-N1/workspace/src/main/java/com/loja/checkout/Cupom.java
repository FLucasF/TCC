package com.loja.checkout;

import java.math.BigDecimal;
import java.util.List;

public enum Cupom {
    BEMVINDO10 {
        @Override public boolean aplicavel(Contexto c) { return true; }
        @Override public BigDecimal desconto(Contexto c) {
            return Dinheiro.centavos(c.subtotalProdutos().multiply(new BigDecimal("0.10")));
        }
    },
    MENOS50 {
        @Override public boolean aplicavel(Contexto c) {
            return c.subtotalProdutos().compareTo(new BigDecimal("300.00")) >= 0;
        }
        @Override public BigDecimal desconto(Contexto c) { return new BigDecimal("50.00"); }
    },
    FRETEGRATIS {
        @Override public boolean aplicavel(Contexto c) { return true; }
        @Override public BigDecimal desconto(Contexto c) { return c.frete(); }
    },
    LEVE3PAGUE2 {
        @Override public boolean aplicavel(Contexto c) { return true; }
        @Override public BigDecimal desconto(Contexto c) {
            BigDecimal total = BigDecimal.ZERO;
            for (Item i : c.itens()) {
                int gratis = i.quantidade() / 3;
                total = total.add(i.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
            }
            return Dinheiro.centavos(total);
        }
    };

    public abstract boolean aplicavel(Contexto c);
    public abstract BigDecimal desconto(Contexto c);

    public record Contexto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {}
}
