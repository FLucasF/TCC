package com.loja.checkout;

import java.math.BigDecimal;

enum Cupom {

    BEMVINDO10 {
        boolean aplicavel(Pedido pedido) {
            return true;
        }

        BigDecimal desconto(Pedido pedido) {
            return Dinheiro.centavos(pedido.subtotal().multiply(new BigDecimal("0.10")));
        }
    },

    MENOS50 {
        boolean aplicavel(Pedido pedido) {
            return pedido.subtotal().compareTo(new BigDecimal("300")) >= 0;
        }

        BigDecimal desconto(Pedido pedido) {
            return Dinheiro.centavos(new BigDecimal("50"));
        }
    },

    FRETEGRATIS {
        boolean aplicavel(Pedido pedido) {
            return true;
        }

        BigDecimal desconto(Pedido pedido) {
            return pedido.frete();
        }
    },

    LEVE3PAGUE2 {
        boolean aplicavel(Pedido pedido) {
            return pedido.itens().stream().anyMatch(item -> item.quantidade() >= 3);
        }

        BigDecimal desconto(Pedido pedido) {
            return pedido.itens().stream()
                    .map(item -> Dinheiro.centavos(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3))))
                    .reduce(Dinheiro.ZERO, BigDecimal::add);
        }
    };

    abstract boolean aplicavel(Pedido pedido);

    abstract BigDecimal desconto(Pedido pedido);
}
