package com.loja.resumocompra;

import java.math.BigDecimal;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal desconto(Contexto contexto) {
            return contexto.subtotal().multiply(new BigDecimal("0.10"));
        }
    },

    MENOS50 {
        @Override
        public boolean aplicavel(Contexto contexto) {
            return contexto.subtotal().compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal desconto(Contexto contexto) {
            return new BigDecimal("50.00");
        }
    },

    FRETEGRATIS {
        @Override
        public BigDecimal desconto(Contexto contexto) {
            return contexto.frete();
        }
    },

    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(Contexto contexto) {
            return contexto.itens().stream().anyMatch(item -> item.quantidade() >= 3);
        }

        @Override
        public BigDecimal desconto(Contexto contexto) {
            return contexto.itens().stream()
                    .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    };

    public boolean aplicavel(Contexto contexto) {
        return true;
    }

    public abstract BigDecimal desconto(Contexto contexto);
}
