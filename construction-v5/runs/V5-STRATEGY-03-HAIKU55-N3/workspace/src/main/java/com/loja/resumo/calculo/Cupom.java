package com.loja.resumo.calculo;

import java.math.BigDecimal;

import static com.loja.resumo.calculo.Dinheiro.ZERO;
import static com.loja.resumo.calculo.Dinheiro.arredonda;

public enum Cupom {

    BEMVINDO10 {
        @Override
        BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return arredonda(pedido.subtotal().multiply(new BigDecimal("0.10")));
        }
    },
    MENOS50 {
        @Override
        boolean aplicavel(Pedido pedido) {
            return pedido.subtotal().compareTo(new BigDecimal("300")) >= 0;
        }

        @Override
        BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return new BigDecimal("50.00");
        }
    },
    FRETEGRATIS {
        @Override
        BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return frete;
        }
    },
    LEVE3PAGUE2 {
        @Override
        BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return pedido.itens().stream()
                    .map(item -> arredonda(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3))))
                    .reduce(ZERO, BigDecimal::add);
        }
    };

    boolean aplicavel(Pedido pedido) {
        return true;
    }

    abstract BigDecimal desconto(Pedido pedido, BigDecimal frete);
}
