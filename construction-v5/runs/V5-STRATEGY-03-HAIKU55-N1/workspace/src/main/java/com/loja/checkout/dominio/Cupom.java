package com.loja.checkout.dominio;

import java.math.BigDecimal;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return Dinheiro.percentual(pedido.subtotal(), "0.10");
        }
    },

    MENOS50 {
        @Override
        public boolean aplicavel(Pedido pedido) {
            return pedido.subtotal().compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return Dinheiro.centavos(new BigDecimal("50.00"));
        }
    },

    FRETEGRATIS {
        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return frete;
        }
    },

    LEVE3PAGUE2 {
        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            BigDecimal soma = pedido.itens().stream()
                    .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                    .reduce(Dinheiro.ZERO, BigDecimal::add);
            return Dinheiro.centavos(soma);
        }
    };

    public boolean aplicavel(Pedido pedido) {
        return true;
    }

    public abstract BigDecimal desconto(Pedido pedido, BigDecimal frete);
}
