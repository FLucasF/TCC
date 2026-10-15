package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Cupons em vigor. Cada promocao traz a propria conta de desconto e a propria
 * condicao de uso.
 */
public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return Dinheiro.percentual(pedido.subtotalProdutos(), new BigDecimal("0.10"));
        }
    },

    MENOS50 {
        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return Dinheiro.arredondar(new BigDecimal("50.00"));
        }

        @Override
        public boolean aplicavel(Pedido pedido) {
            return pedido.subtotalProdutos().compareTo(new BigDecimal("300.00")) >= 0;
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
            BigDecimal gratis = pedido.itens().stream()
                    .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return Dinheiro.arredondar(gratis);
        }
    };

    /** Desconto do cupom neste pedido, em centavos. */
    public abstract BigDecimal desconto(Pedido pedido, BigDecimal frete);

    /** Por padrao o cupom vale para qualquer pedido. */
    public boolean aplicavel(Pedido pedido) {
        return true;
    }
}
