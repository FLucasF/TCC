package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Cada cupom sabe se o pedido cumpre a condicao dele e quanto desconta.
 * A assinatura recebe pedido e frete porque ha cupom que olha o subtotal,
 * cupom que olha item a item e cupom que zera o frete.
 */
public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return Dinheiro.centavos(pedido.subtotalProdutos().multiply(new BigDecimal("0.10")));
        }
    },

    MENOS50 {
        @Override
        public boolean aplicavel(Pedido pedido, BigDecimal frete) {
            return pedido.subtotalProdutos().compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return Dinheiro.centavos(new BigDecimal("50.00"));
        }
    },

    FRETEGRATIS {
        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return Dinheiro.centavos(frete);
        }
    },

    LEVE3PAGUE2 {
        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            BigDecimal desconto = pedido.itens().stream()
                    .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return Dinheiro.centavos(desconto);
        }
    };

    public boolean aplicavel(Pedido pedido, BigDecimal frete) {
        return true;
    }

    public abstract BigDecimal desconto(Pedido pedido, BigDecimal frete);
}
