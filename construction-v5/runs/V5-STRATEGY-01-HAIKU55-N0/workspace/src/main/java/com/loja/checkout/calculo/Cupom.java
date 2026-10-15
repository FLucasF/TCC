package com.loja.checkout.calculo;

import java.math.BigDecimal;
import java.util.List;

import static com.loja.checkout.calculo.Dinheiro.centavos;

public enum Cupom {

    BEMVINDO10 {
        @Override
        boolean aplicavel(List<ItemPedido> itens, BigDecimal subtotal) {
            return true;
        }

        @Override
        BigDecimal desconto(List<ItemPedido> itens, BigDecimal subtotal, BigDecimal frete) {
            return centavos(subtotal.multiply(new BigDecimal("0.10")));
        }
    },
    MENOS50 {
        @Override
        boolean aplicavel(List<ItemPedido> itens, BigDecimal subtotal) {
            return subtotal.compareTo(new BigDecimal("300")) >= 0;
        }

        @Override
        BigDecimal desconto(List<ItemPedido> itens, BigDecimal subtotal, BigDecimal frete) {
            return new BigDecimal("50.00");
        }
    },
    FRETEGRATIS {
        @Override
        boolean aplicavel(List<ItemPedido> itens, BigDecimal subtotal) {
            return true;
        }

        @Override
        BigDecimal desconto(List<ItemPedido> itens, BigDecimal subtotal, BigDecimal frete) {
            return frete;
        }
    },
    LEVE3PAGUE2 {
        @Override
        boolean aplicavel(List<ItemPedido> itens, BigDecimal subtotal) {
            return itens.stream().anyMatch(item -> item.quantidade() >= 3);
        }

        @Override
        BigDecimal desconto(List<ItemPedido> itens, BigDecimal subtotal, BigDecimal frete) {
            BigDecimal total = BigDecimal.ZERO;
            for (ItemPedido item : itens) {
                int gratis = item.quantidade() / 3;
                total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
            }
            return centavos(total);
        }
    };

    abstract boolean aplicavel(List<ItemPedido> itens, BigDecimal subtotal);

    abstract BigDecimal desconto(List<ItemPedido> itens, BigDecimal subtotal, BigDecimal frete);
}
