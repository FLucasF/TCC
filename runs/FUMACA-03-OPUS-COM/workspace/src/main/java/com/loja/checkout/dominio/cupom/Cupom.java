package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Cupons promocionais. Cada um define sozinho a própria condição de uso e o
 * próprio desconto, sobre o que precisar do pedido.
 */
public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal desconto(PedidoParaCupom pedido) {
            return Dinheiro.percentual(pedido.subtotalProdutos(), "0.10");
        }
    },

    MENOS50 {
        private static final BigDecimal MINIMO_EM_PRODUTOS = new BigDecimal("300.00");

        @Override
        public boolean aplicavel(PedidoParaCupom pedido) {
            return pedido.subtotalProdutos().compareTo(MINIMO_EM_PRODUTOS) >= 0;
        }

        @Override
        public BigDecimal desconto(PedidoParaCupom pedido) {
            return Dinheiro.valor("50.00");
        }
    },

    FRETEGRATIS {
        @Override
        public BigDecimal desconto(PedidoParaCupom pedido) {
            return Dinheiro.valor(pedido.frete());
        }
    },

    LEVE3PAGUE2 {
        @Override
        public BigDecimal desconto(PedidoParaCupom pedido) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (ItemPedido item : pedido.itens()) {
                BigDecimal gratis = BigDecimal.valueOf(item.quantidade() / 3);
                desconto = desconto.add(item.precoUnitario().multiply(gratis));
            }
            return Dinheiro.valor(desconto);
        }
    };

    public static Optional<Cupom> porCodigo(String codigo) {
        for (Cupom cupom : values()) {
            if (cupom.name().equals(codigo)) {
                return Optional.of(cupom);
            }
        }
        return Optional.empty();
    }

    public abstract BigDecimal desconto(PedidoParaCupom pedido);

    /** Por padrão o cupom vale para qualquer pedido; quem tem condição sobrescreve. */
    public boolean aplicavel(PedidoParaCupom pedido) {
        return true;
    }
}
