package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.List;

public enum Cupom {
    BEMVINDO10 {
        @Override public boolean aplicavel(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
            return true;
        }
        @Override public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
            return Dinheiro.cent(subtotal.multiply(new BigDecimal("0.10")));
        }
    },
    MENOS50 {
        @Override public boolean aplicavel(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
            return subtotal.compareTo(new BigDecimal("300.00")) >= 0;
        }
        @Override public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
            return new BigDecimal("50.00");
        }
    },
    FRETEGRATIS {
        @Override public boolean aplicavel(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
            return true;
        }
        @Override public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
            return frete;
        }
    },
    LEVE3PAGUE2 {
        @Override public boolean aplicavel(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
            return true;
        }
        @Override public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
            BigDecimal d = BigDecimal.ZERO;
            for (Item i : itens) {
                int gratis = i.quantidade() / 3;
                if (gratis > 0) {
                    d = d.add(i.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
                }
            }
            return Dinheiro.cent(d);
        }
    };

    public abstract boolean aplicavel(List<Item> itens, BigDecimal subtotal, BigDecimal frete);
    public abstract BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete);
}
