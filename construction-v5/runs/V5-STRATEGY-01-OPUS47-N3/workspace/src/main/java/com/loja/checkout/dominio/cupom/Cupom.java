package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;

import java.math.BigDecimal;
import java.util.List;

public enum Cupom {
    BEMVINDO10 {
        @Override public BigDecimal desconto(BigDecimal subtotal, List<Item> itens, BigDecimal frete) {
            return Dinheiro.aCentavos(subtotal.multiply(new BigDecimal("0.10")));
        }
        @Override public boolean aplicavel(BigDecimal subtotal) { return true; }
    },
    MENOS50 {
        @Override public BigDecimal desconto(BigDecimal subtotal, List<Item> itens, BigDecimal frete) {
            return Dinheiro.aCentavos(new BigDecimal("50.00"));
        }
        @Override public boolean aplicavel(BigDecimal subtotal) {
            return subtotal.compareTo(new BigDecimal("300.00")) >= 0;
        }
    },
    FRETEGRATIS {
        @Override public BigDecimal desconto(BigDecimal subtotal, List<Item> itens, BigDecimal frete) {
            return frete;
        }
        @Override public boolean aplicavel(BigDecimal subtotal) { return true; }
    },
    LEVE3PAGUE2 {
        @Override public BigDecimal desconto(BigDecimal subtotal, List<Item> itens, BigDecimal frete) {
            BigDecimal total = BigDecimal.ZERO;
            for (Item it : itens) {
                int gratis = it.quantidade() / 3;
                total = total.add(it.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
            }
            return Dinheiro.aCentavos(total);
        }
        @Override public boolean aplicavel(BigDecimal subtotal) { return true; }
    };

    public abstract BigDecimal desconto(BigDecimal subtotal, List<Item> itens, BigDecimal frete);
    public abstract boolean aplicavel(BigDecimal subtotal);
}
