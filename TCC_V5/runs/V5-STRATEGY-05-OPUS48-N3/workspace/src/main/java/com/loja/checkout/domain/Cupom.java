package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cupons promocionais. Cada um define sua condição de aplicação e seu desconto.
 * A assinatura recebe itens, subtotal e frete porque é o que o cupom mais
 * exigente precisa (FRETEGRATIS usa o frete; MENOS50 usa o subtotal;
 * LEVE3PAGUE2 usa os itens).
 */
public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
            return Dinheiro.centavos(subtotal.multiply(new BigDecimal("0.10")));
        }
    },
    MENOS50 {
        @Override
        public boolean aplicavel(BigDecimal subtotal) {
            return subtotal.compareTo(new BigDecimal("300.00")) >= 0;
        }

        @Override
        public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
            return new BigDecimal("50.00");
        }
    },
    FRETEGRATIS {
        @Override
        public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
            return frete;
        }
    },
    LEVE3PAGUE2 {
        @Override
        public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
            BigDecimal total = BigDecimal.ZERO;
            for (Item item : itens) {
                int gratis = item.quantidade() / 3;
                total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
            }
            return Dinheiro.centavos(total);
        }
    };

    /** Se o pedido cumpre a condição do cupom. */
    public boolean aplicavel(BigDecimal subtotal) {
        return true;
    }

    /** Valor do desconto, já arredondado para centavos. */
    public abstract BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete);
}
