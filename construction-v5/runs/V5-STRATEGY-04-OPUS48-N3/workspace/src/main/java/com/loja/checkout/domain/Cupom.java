package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/**
 * Cada cupom tem sua regra de desconto e sua condição de uso. Promoção nova =
 * nova constante aqui.
 */
public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal freteEfetivo) {
            return Dinheiro.arredonda(pedido.subtotalProdutos().multiply(new BigDecimal("0.10")));
        }
    },
    MENOS50 {
        @Override
        public boolean aplicavel(Pedido pedido) {
            return pedido.subtotalProdutos().compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal freteEfetivo) {
            return new BigDecimal("50.00");
        }
    },
    FRETEGRATIS {
        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal freteEfetivo) {
            return freteEfetivo;
        }
    },
    LEVE3PAGUE2 {
        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal freteEfetivo) {
            BigDecimal total = BigDecimal.ZERO;
            for (Pedido.Item item : pedido.itens()) {
                int gratis = item.quantidade() / 3;
                total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
            }
            return Dinheiro.arredonda(total);
        }
    };

    /** O pedido cumpre a condição do cupom? Por padrão, sim. */
    public boolean aplicavel(Pedido pedido) {
        return true;
    }

    public abstract BigDecimal desconto(Pedido pedido, BigDecimal freteEfetivo);

    public static Optional<Cupom> fromCodigo(String codigo) {
        return Arrays.stream(values()).filter(c -> c.name().equals(codigo)).findFirst();
    }
}
