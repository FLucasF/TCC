package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Cupom de desconto sobre o valor dos produtos. Só um por pedido.
 *
 * Cada cupom sabe dizer se o pedido cumpre sua condição ({@link #aplicavel})
 * e quanto desconta ({@link #desconto}). Novas promoções entram como constantes.
 */
public enum Cupom {

    /** 10% de desconto no valor dos produtos. */
    BEMVINDO10 {
        @Override
        public BigDecimal desconto(BigDecimal subtotal, List<Item> itens, BigDecimal frete) {
            return Dinheiro.arredondar(subtotal.multiply(BigDecimal.valueOf(0.10)));
        }
    },

    /** R$ 50,00 de desconto, só a partir de R$ 300,00 em produtos. */
    MENOS50 {
        @Override
        public boolean aplicavel(BigDecimal subtotal) {
            return subtotal.compareTo(BigDecimal.valueOf(300)) >= 0;
        }

        @Override
        public BigDecimal desconto(BigDecimal subtotal, List<Item> itens, BigDecimal frete) {
            return Dinheiro.valor(50);
        }
    },

    /** O cliente não paga o frete: o desconto fica igual ao valor do frete. */
    FRETEGRATIS {
        @Override
        public BigDecimal desconto(BigDecimal subtotal, List<Item> itens, BigDecimal frete) {
            return Dinheiro.arredondar(frete);
        }
    },

    /** A cada 3 unidades de um mesmo item, uma sai de graça. */
    LEVE3PAGUE2 {
        @Override
        public BigDecimal desconto(BigDecimal subtotal, List<Item> itens, BigDecimal frete) {
            BigDecimal total = BigDecimal.ZERO;
            for (Item item : itens) {
                int gratis = item.quantidade() / 3;
                total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
            }
            return Dinheiro.arredondar(total);
        }
    };

    public static Optional<Cupom> fromCodigo(String codigo) {
        return Enums.fromNome(Cupom.class, codigo);
    }

    /** Se o pedido cumpre a condição do cupom. Padrão: sempre. */
    public boolean aplicavel(BigDecimal subtotal) {
        return true;
    }

    /** Desconto já arredondado para centavos. */
    public abstract BigDecimal desconto(BigDecimal subtotal, List<Item> itens, BigDecimal frete);
}
