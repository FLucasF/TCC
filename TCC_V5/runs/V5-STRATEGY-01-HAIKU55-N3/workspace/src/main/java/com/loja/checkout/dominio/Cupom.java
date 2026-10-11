package com.loja.checkout.dominio;

import java.math.BigDecimal;

public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return Dinheiro.percentual(pedido.subtotal(), new BigDecimal("0.10"));
        }
    },

    MENOS50 {
        @Override
        public void validarPara(Pedido pedido) {
            if (pedido.subtotal().compareTo(MINIMO_PEDIDO) < 0) {
                throw new RecusaPedido(Erro.CUPOM_NAO_APLICAVEL);
            }
        }

        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return new BigDecimal("50.00");
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
            return Dinheiro.centavos(pedido.itens().stream()
                    .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
    };

    private static final BigDecimal MINIMO_PEDIDO = new BigDecimal("300");

    public void validarPara(Pedido pedido) {
    }

    public abstract BigDecimal desconto(Pedido pedido, BigDecimal frete);
}
